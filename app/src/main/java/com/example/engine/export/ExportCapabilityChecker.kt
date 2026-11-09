package com.example.engine.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

data class ResolutionCapability(
    val preset: ResolutionPreset,
    val isSupported: Boolean,
    val warningMessage: String? = null,
    val recommendedCodec: String = "video/avc"
)

object ExportCapabilityChecker {

    fun checkAllCapabilities(fps: Int = 30): Map<ResolutionPreset, ResolutionCapability> {
        val result = mutableMapOf<ResolutionPreset, ResolutionCapability>()
        val presets = listOf(
            ResolutionPreset.RES_720P,
            ResolutionPreset.RES_1080P,
            ResolutionPreset.RES_2K,
            ResolutionPreset.RES_4K
        )

        val bestMime = findBestEncoderMime()

        for (preset in presets) {
            val (supported, warning) = checkResolutionSupport(preset.width, preset.height, fps, bestMime)
            result[preset] = ResolutionCapability(
                preset = preset,
                isSupported = supported,
                warningMessage = warning,
                recommendedCodec = bestMime
            )
        }

        return result
    }

    fun findBestEncoderMime(): String {
        val preferredMimes = listOf("video/avc", "video/hevc", "video/x-vnd.on2.vp9")
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (mime in preferredMimes) {
                for (info in codecList.codecInfos) {
                    if (info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }) {
                        return mime
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return "video/avc"
    }

    fun checkResolutionSupport(width: Int, height: Int, fps: Int, mime: String): Pair<Boolean, String?> {
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                if (!info.supportedTypes.any { it.equals(mime, ignoreCase = true) }) continue

                val caps = info.getCapabilitiesForType(mime)
                val videoCaps = caps.videoCapabilities ?: continue

                // Check size support (check both orientations)
                val directSupported = videoCaps.isSizeSupported(width, height)
                val rotatedSupported = videoCaps.isSizeSupported(height, width)

                if (directSupported || rotatedSupported) {
                    val rateSupported = videoCaps.areSizeAndRateSupported(
                        if (directSupported) width else height,
                        if (directSupported) height else width,
                        fps.toDouble()
                    )
                    return if (rateSupported) {
                        Pair(true, null)
                    } else {
                        Pair(false, "${width}x${height} at ${fps}fps exceeds device maximum framerate capacity")
                    }
                }
            }
        } catch (e: Exception) {
            return Pair(true, null) // Allow fallback rendering
        }

        return if (width >= 2160 || height >= 3840) {
            Pair(false, "4K ${fps}fps is not supported by this device's hardware encoder. 1080p is recommended.")
        } else if (width >= 1440 || height >= 2560) {
            Pair(false, "2K resolution exceeds maximum hardware encoding dimensions.")
        } else {
            Pair(true, null)
        }
    }
}
