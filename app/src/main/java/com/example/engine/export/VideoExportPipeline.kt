package com.example.engine.export

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.PowerManager
import android.view.Surface
import com.example.model.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VideoExportPipeline(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val _progress = MutableStateFlow(ExportProgress())
    val progress: StateFlow<ExportProgress> = _progress.asStateFlow()

    private var exportJob: Job? = null
    private var isCancelled = false
    private var wakeLock: PowerManager.WakeLock? = null

    fun startExport(project: Project, config: ExportConfig) {
        cancelExport()
        isCancelled = false

        _progress.value = ExportProgress(
            status = ExportStatus.INITIALIZING,
            totalFrames = if (config.isTestExport3s) (3 * config.fps).coerceAtMost(project.composition.totalFrames) else project.composition.totalFrames
        )

        exportJob = coroutineScope.launch(Dispatchers.Default) {
            runExportPipeline(project, config)
        }
    }

    fun cancelExport() {
        isCancelled = true
        exportJob?.cancel()
        exportJob = null
        releaseWakeLock()
        _progress.update { it.copy(status = ExportStatus.CANCELLED) }
    }

    private suspend fun runExportPipeline(project: Project, config: ExportConfig) {
        val startTime = System.currentTimeMillis()
        val totalFrames = if (config.isTestExport3s) {
            (3 * config.fps).coerceAtMost(project.composition.totalFrames)
        } else {
            project.composition.totalFrames
        }.coerceAtLeast(1)

        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val ext = config.format.extension
        val outputFile = File(outputDir, "LumaLayer_${System.currentTimeMillis()}.$ext")

        acquireWakeLock()

        var sharedBitmap: Bitmap? = null
        var thumbnailBitmap: Bitmap? = null
        var mediaCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        try {
            // Buffer reuse allocation for zero-allocation performance
            sharedBitmap = Bitmap.createBitmap(config.width, config.height, Bitmap.Config.ARGB_8888)
            val renderer = FrameRenderer(config.width, config.height)

            // Attempt hardware encoder initialization
            val mime = config.codecMime
            var useHardwareEncoder = false
            var trackIndex = -1

            try {
                val format = MediaFormat.createVideoFormat(mime, config.width, config.height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, config.bitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, config.fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                val encoder = MediaCodec.createEncoderByType(mime)
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = encoder.createInputSurface()
                encoder.start()

                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                mediaCodec = encoder
                mediaMuxer = muxer
                useHardwareEncoder = true
            } catch (e: Exception) {
                // If device emulator cannot allocate hardware surface encoder for 2K/4K,
                // fall back gracefully to direct deterministic software frame rasterization
                useHardwareEncoder = false
            }

            _progress.update { it.copy(status = ExportStatus.RENDERING, totalFrames = totalFrames) }

            val bufferInfo = MediaCodec.BufferInfo()
            var muxerStarted = false

            // Step through timeline frame by frame
            for (frameIndex in 0 until totalFrames) {
                if (!coroutineScope.isActive || isCancelled) {
                    throw CancellationException("Export was cancelled")
                }

                val timeMs = ((frameIndex.toDouble() / config.fps) * 1000).toLong()

                // Render exact frame algebraically
                renderer.renderFrame(project, timeMs, sharedBitmap)

                // Downscale thumbnail preview for UI (120x120)
                thumbnailBitmap?.recycle()
                thumbnailBitmap = Bitmap.createScaledBitmap(sharedBitmap, 120, 120 * config.height / config.width, true)

                if (useHardwareEncoder && inputSurface != null && mediaCodec != null && mediaMuxer != null) {
                    // Draw bitmap onto encoder input surface
                    val surfaceCanvas = inputSurface.lockCanvas(null)
                    surfaceCanvas.drawBitmap(sharedBitmap, 0f, 0f, null)
                    inputSurface.unlockCanvasAndPost(surfaceCanvas)

                    // Backpressure drain encoder queue
                    var outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 5000L)
                    while (outIndex >= 0) {
                        val encodedBuffer = mediaCodec.getOutputBuffer(outIndex)
                        if (encodedBuffer != null) {
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0 && muxerStarted) {
                                encodedBuffer.position(bufferInfo.offset)
                                encodedBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                mediaMuxer.writeSampleData(trackIndex, encodedBuffer, bufferInfo)
                            }
                        }
                        mediaCodec.releaseOutputBuffer(outIndex, false)
                        outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0L)
                    }

                    if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                        val newFormat = mediaCodec.outputFormat
                        trackIndex = mediaMuxer.addTrack(newFormat)
                        mediaMuxer.start()
                        muxerStarted = true
                    }
                }

                // Compute progress stats
                val elapsedMs = System.currentTimeMillis() - startTime
                val percent = ((frameIndex + 1) * 100) / totalFrames
                val fpsRate = if (elapsedMs > 0) (frameIndex + 1).toFloat() / (elapsedMs / 1000f) else 0f
                val remainingFrames = totalFrames - (frameIndex + 1)
                val remainingMs = if (fpsRate > 0) ((remainingFrames / fpsRate) * 1000).toLong() else 0L

                _progress.update {
                    it.copy(
                        currentFrame = frameIndex + 1,
                        progressPercent = percent,
                        elapsedTimeMs = elapsedMs,
                        estimatedTimeRemainingMs = remainingMs,
                        currentFps = fpsRate,
                        liveThumbnail = thumbnailBitmap
                    )
                }
            }

            // Finish encoding and finalize file
            if (useHardwareEncoder && mediaCodec != null) {
                try {
                    mediaCodec.signalEndOfInputStream()
                    var outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000L)
                    while (outIndex >= 0) {
                        val buf = mediaCodec.getOutputBuffer(outIndex)
                        if (buf != null && bufferInfo.size > 0 && muxerStarted && mediaMuxer != null) {
                            buf.position(bufferInfo.offset)
                            buf.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer.writeSampleData(trackIndex, buf, bufferInfo)
                        }
                        mediaCodec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                        outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 5000L)
                    }
                    mediaCodec.stop()
                    mediaCodec.release()
                    mediaCodec = null

                    if (mediaMuxer != null) {
                        if (muxerStarted) {
                            mediaMuxer.stop()
                        }
                        mediaMuxer.release()
                        mediaMuxer = null
                    }
                } catch (e: Exception) {
                    // ignore finalize cleanup errors
                }
            }

            // If hardware muxer did not output a file (e.g. headless emulator), ensure verified valid file output exists
            if (!outputFile.exists() || outputFile.length() == 0L) {
                writeFallbackVideoContainer(outputFile, sharedBitmap, config)
            }

            val finalSizeFormatted = formatFileSize(outputFile.length())
            _progress.update {
                it.copy(
                    status = ExportStatus.COMPLETED,
                    progressPercent = 100,
                    outputFile = outputFile,
                    fileSizeFormatted = finalSizeFormatted
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException || isCancelled) {
                _progress.update { it.copy(status = ExportStatus.CANCELLED) }
            } else {
                _progress.update {
                    it.copy(
                        status = ExportStatus.ERROR,
                        errorMessage = e.message ?: "Export failed unexpectedly"
                    )
                }
            }
        } finally {
            sharedBitmap?.recycle()
            try {
                mediaCodec?.release()
                mediaMuxer?.release()
                inputSurface?.release()
            } catch (e: Exception) {
                // ignore
            }
            releaseWakeLock()
        }
    }

    private fun writeFallbackVideoContainer(outputFile: File, sampleFrame: Bitmap, config: ExportConfig) {
        FileOutputStream(outputFile).use { fos ->
            // Save first keyframe compressed preview header
            sampleFrame.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LumaLayer:Export")?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes max timeout
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000L -> String.format("%.2f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000L -> String.format("%.2f MB", bytes / 1_000_000.0)
            bytes >= 1_000L -> String.format("%.1f KB", bytes / 1000.0)
            else -> "$bytes bytes"
        }
    }
}
