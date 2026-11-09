package com.example.model

/**
 * High-precision speed ramping and optical time-remapping presets for After Effects-style timing control.
 */
enum class SpeedRampPreset(val label: String, val description: String) {
    SLOW_MO_20("Slow-Mo Matrix (20%)", "Ultra smooth 1/5th slow-motion bullet-time playback."),
    SLOW_MO_50("Half Speed (50%)", "Smooth cinematic 50% half-speed playback."),
    FAST_FORWARD_200("Hyper Speed (2x)", "High-energy 200% double-speed fast forward."),
    FAST_SLOW_FAST("Bullet-Time Ramp", "Speed ramp: 250% speed entrance -> 25% bullet-time slow-mo -> 250% exit."),
    REVERSE("Reverse Playback", "Plays the layer in reverse from end frame to start frame."),
    FREEZE_FRAME("Freeze Frame", "Freezes layer playback indefinitely at the current playhead frame.")
}
