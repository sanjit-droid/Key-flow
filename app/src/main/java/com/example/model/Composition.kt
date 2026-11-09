package com.example.model

/**
 * Defines the composition settings such as resolution, framerate, and total duration.
 */
data class Composition(
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val durationMs: Long = 5000L,
    val backgroundColorHex: String = "#0A0B10"
) {
    val totalFrames: Int
        get() = ((durationMs * fps) / 1000).toInt()

    fun frameToTimeMs(frame: Int): Long {
        return ((frame.toDouble() / fps) * 1000).toLong().coerceIn(0L, durationMs)
    }

    fun timeMsToFrame(timeMs: Long): Int {
        return ((timeMs.toDouble() / 1000) * fps).toInt().coerceIn(0, totalFrames)
    }
}
