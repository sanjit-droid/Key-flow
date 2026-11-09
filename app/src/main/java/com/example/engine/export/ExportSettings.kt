package com.example.engine.export

import java.io.File

enum class ResolutionPreset(val label: String, val width: Int, val height: Int) {
    RES_720P("720p HD (720x1280)", 720, 1280),
    RES_1080P("1080p Full HD (1080x1920)", 1080, 1920),
    RES_2K("2K QHD (1440x2560)", 1440, 2560),
    RES_4K("4K UHD (2160x3840)", 2160, 3840),
    CUSTOM("Custom Resolution", 1080, 1920)
}

enum class ExportQuality(val label: String, val multiplier: Float) {
    LOW("Low", 0.6f),
    MEDIUM("Medium", 1.0f),
    HIGH("High", 1.6f),
    MAX("Max", 2.5f);

    fun defaultBitrateBps(width: Int, height: Int, fps: Int): Int {
        // Base bitrate targets:
        // 1080p 30fps High = ~16 Mbps (16,000,000)
        // 2K 30fps High = ~30 Mbps
        // 4K 30fps High = ~60 Mbps
        val pixelRate = (width.toLong() * height.toLong() * fps.toLong())
        val baseBps = when {
            pixelRate >= 2160L * 3840L * 24L -> (38_000_000 * multiplier).toInt() // 4K ~60 Mbps at High
            pixelRate >= 1440L * 2560L * 24L -> (19_000_000 * multiplier).toInt() // 2K ~30 Mbps at High
            pixelRate >= 1080L * 1920L * 24L -> (10_000_000 * multiplier).toInt() // 1080p ~16 Mbps at High
            else -> (5_000_000 * multiplier).toInt() // 720p ~8 Mbps
        }
        return baseBps.coerceIn(1_000_000, 100_000_000)
    }
}

enum class ExportFormat(val label: String, val extension: String, val defaultMime: String) {
    MP4("MP4 (H.264 / AVC)", "mp4", "video/avc"),
    WEBM("WebM (VP9)", "webm", "video/x-vnd.on2.vp9")
}

data class ExportConfig(
    val resolutionPreset: ResolutionPreset = ResolutionPreset.RES_1080P,
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val quality: ExportQuality = ExportQuality.HIGH,
    val bitrateBps: Int = 16_000_000,
    val format: ExportFormat = ExportFormat.MP4,
    val codecMime: String = "video/avc",
    val isTestExport3s: Boolean = false
) {
    val totalSecondsToRender: Float
        get() = if (isTestExport3s) 3.0f else 0f
}

enum class ExportStatus {
    IDLE,
    CHECKING_CAPABILITIES,
    INITIALIZING,
    RENDERING,
    COMPLETED,
    CANCELLED,
    ERROR
}

data class ExportProgress(
    val status: ExportStatus = ExportStatus.IDLE,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0,
    val progressPercent: Int = 0,
    val elapsedTimeMs: Long = 0L,
    val estimatedTimeRemainingMs: Long = 0L,
    val currentFps: Float = 0f,
    val liveThumbnail: android.graphics.Bitmap? = null,
    val errorMessage: String? = null,
    val outputFile: File? = null,
    val fileSizeFormatted: String? = null
)
