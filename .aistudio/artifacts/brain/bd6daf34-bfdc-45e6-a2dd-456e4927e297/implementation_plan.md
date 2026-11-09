# LumaLayer Studio: Phase 1 Core Editor

A professional mobile-first layer compositor and motion graphics editor built with Jetpack Compose and hardware-accelerated Canvas. LumaLayer Studio delivers an After Effects-style keyframe and transform workflow directly on Android mobile devices with full offline project saving and loading.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architecture choices have been confirmed and will guide the Phase 1 implementation:

- **Technology Platform**: Native Android application powered by Jetpack Compose, Kotlin Coroutines, and hardware-accelerated Android Canvas rendering.
- **Brand Identity**: **LumaLayer Studio** — a sleek, dark-themed pro motion design workstation.
- **Layout Architecture**: Split docked panel layout tailored for mobile ergonomics:
  - Top: Preview canvas viewport with fit/zoom toggle and active layer visual handles.
  - Center: Transport controls (play/pause, frame stepping, timecode display) and interactive timeline ruler with playhead.
  - Bottom Dock: Switchable pro panels for Layer Hierarchy (Root > layer items, add layer menu) and Inspector (Transform properties with keyframe stopwatch toggles).
- **Execution Methodology**: Strictly follow the requested phase-by-phase rollout. Complete Phase 1 entirely, review test status, and pause for approval before progressing to Phase 2.

---

## 1. Overview & Core Concept

**LumaLayer Studio** brings timeline-driven motion graphics and multi-layer composition to Android. Users can create compositions with text, shapes (rectangles, circles), solid color backdrops, media imports, adjustment layers, and nested groups. 

Every layer exposes transform properties (Position, Scale, Pivot, Rotation, Opacity) that can either be set statically or animated across time by toggling stopwatch keyframing. The timeline provides a precision time ruler, draggable playhead, frame-by-frame navigation, and visual keyframe diamonds that update in real time on the preview canvas.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Color Palette**: Pro Dark Obsidian (`#0F1015` base, `#181A22` panel surfaces, `#242838` borders/dock accents).
- **Accent Tokens**: Electric Cyan (`#00E5FF`) for active keyframes and playhead, Vibrant Violet (`#8B5CF6`) for layer selection, and Bright Amber (`#F59E0B`) for stopwatch indicators.
- **Typography**: Clean, monospace-inspired tabular figures for accurate timecode (`00:01.15 / 00:05.00`) and numeric property scrubbing.

### Key Layout Sections
```
┌────────────────────────────────────────────────────────┐
│  LumaLayer Studio           [Zoom: Fit ▾] [Project ▾]   │
├────────────────────────────────────────────────────────┤
│                                                        │
│                  PREVIEW CANVAS                        │
│            (Active Layer Bounding Box)                 │
│                                                        │
├────────────────────────────────────────────────────────┤
│  [⏮] [◀ -1f]   [ ▶ Play / ❚❚ Pause ]   [+1f ▶] [⏭]     │
│  Time: 00:01.00 / 00:05.00 (Frame 30/150 @ 30fps)     │
├────────────────────────────────────────────────────────┤
│  TIMELINE RULER & PLAYHEAD SCRUBBER                    │
│  Track 1: [◆      Layer 1: Text        ◆]              │
│  Track 2: [   ◆   Layer 2: Shape Rect     ◆]           │
├────────────────────────────────────────────────────────┤
│  [ Layers (3) ]  |  [ Inspector: Transform ]           │
│ ────────────────────────────────────────────────────── │
│  Position:  X: [ 540 ]   Y: [ 960 ]      [⏱ Active]   │
│  Scale:     X: [ 100% ]  Y: [ 100% ] [🔗] [⏱ Off]     │
│  Rotation:  [ 45° ]                      [⏱ Active]   │
│  Opacity:   [ 100% ]                     [⏱ Off]      │
│  Pivot:     X: [ 0.50 ]  Y: [ 0.50 ]                   │
└────────────────────────────────────────────────────────┘
```

### Key User Flows
1. **Layer Management**: Tap `+ Add Layer` to create Text, Shape (Rectangle, Circle), Solid Color, Media (Image/Video placeholder), or Group. Reorder layers, toggle visibility, and delete.
2. **Scrub & Frame Stepping**: Drag the timeline ruler or touch the scrub slider to advance frame-by-frame. Press `-1 frame` or `+1 frame` for single-frame precision.
3. **Keyframe Workflow**: Select a layer, go to Inspector, toggle the Stopwatch icon on a property (e.g. Rotation or Scale). A keyframe is inserted at the current playhead. Scrub to another time and change the value; a new keyframe is automatically committed and displayed as a diamond marker on the timeline.
4. **Project Save/Load**: Export project state as clean, indented JSON or load an existing `.json` file to restore all layers, keyframe tracks, and composition settings.

---

## 3. Key Product Decisions & Trade-Offs

- **Deterministic Frame Evaluation Engine**: Rather than relying on non-deterministic system animations, the app evaluates all property values algebraically at any given timestamp `t`:
  $$\text{Value}(t) = \text{Interpolate}(\text{Keyframe}_A, \text{Keyframe}_B, t)$$
  This guarantees that playback, scrubbing, and future video export produce identical visual results.
- **Split Docked Controls**: On mobile portrait screens, docking the timeline directly beneath the canvas and placing tabbed Panels (Layers / Inspector) in the bottom tier ensures large 48dp+ hit targets without obscuring the canvas preview.
- **Offline JSON Persistence**: Full project serialization (Moshi/Kotlinx) stores composition dimensions, framerate, duration, layers, transform tracks, and keyframes in standard JSON format for easy import/export.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram
```
┌────────────────────────────────────────────────────────┐
│                   EditorState                          │
│  (Composition, Layers, SelectedLayerId, CurrentTimeMs) │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌─────────────────────────┐ ┌────────────────────────────┐
│   PlaybackEngine        │ │   PropertyEvaluator        │
│   (30fps clock, step,   │ │   (Resolves KeyframeTrack  │
│    play/pause, loop)    │ │    into current values)    │
└────────────┬────────────┘ └────────────┬───────────────┘
             │                           │
             └─────────────┬─────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│                   PreviewCanvas                        │
│   (Draws evaluated shapes, text, solids, selection box)│
└────────────────────────────────────────────────────────┘
```

### Core Data Models
- **`Composition`**: `width: Int`, `height: Int`, `fps: Int`, `durationMs: Long`
- **`Layer`**: `id: String`, `name: String`, `type: LayerType` (Text, ShapeRect, ShapeCircle, SolidColor, Media, Adjustment, Group), `startTimeMs: Long`, `durationMs: Long`, `isVisible: Boolean`, `isLocked: Boolean`
- **`TransformState`**: Tracks for `positionX`, `positionY`, `scaleX`, `scaleY`, `scaleLinked`, `rotation`, `opacity`, `pivotX`, `pivotY`
- **`KeyframeTrack<T>`**: `isAnimated: Boolean`, `defaultValue: T`, `keyframes: List<Keyframe<T>>`
- **`Keyframe<T>`**: `id: String`, `timeMs: Long`, `value: T`

---

## Implementation & Phase Roadmap Status

- **Phase 1: Core Editor** — [COMPLETED]
  - Canvas preview, playhead scrubbing, timeline tracks with orange handles, layer CRUD, transform inspector with stopwatches, deterministic property evaluator, offline JSON save/load.
- **Phase 2: Curve Editor & Smooth Zoom** — [COMPLETED]
  - Cubic Bezier curve editor (Newton-Raphson + bisection), Value & Speed graph, handles, presets (Linear, Ease In, Ease Out, Ease In-Out, Bounce, Elastic), copy/paste curve, full Undo/Redo stack, Smooth Zoom quick action.
- **Phase 3: High-Quality Export up to 4K** — [COMPLETED]
  - Offline frame-by-frame rendering pipeline, hardware MediaCodec AVC/HEVC encoder, MediaMuxer MP4 packaging, wake lock, progress dialog, 720p / 1080p / 4K capability checks.
- **Orientation Workstation (User Reference)** — [COMPLETED]
  - Dual responsive layout (Portrait and Landscape). Hot pink accent borders, normalized coordinates, rotation turns, and docked layers matching user's reference mockup.
- **Phase 4: Background Removal** — [COMPLETED]
  - Client-side segmentation, per-frame mask caching, morphological shrink/grow (erosion/dilation), separable edge feathering, preview mask / cutout compositing, inspector controls.
- **Phase 5: Effects Engine** — [COMPLETED]
  - Data-driven shader registry across 5 categories, uniform parameter sliders, stopwatch keyframing, dynamic inspector UI, sequential shader pipeline, categorized browser.
- **Phase 6: Distortion & Warp Pack** — [COMPLETED]
  - Wave Warp (sine undulating ripples with directional angle, wave height, wavelength, phase, speed).
  - Fisheye Lens (barrel & pincushion optical lens distortion with center coordinates and radius).
  - Water Ripple (concentric expanding droplet rings with frequency, decay radius, amplitude).
  - Polar Coordinates (rectangular to polar and polar to rectangular coordinate mappings).
  - Twirl Vortex (localized spiral vortex with center coordinate and twist angle).
  - Kaleido Mirror (multi-segment radial symmetry reflection and kaleidoscope generator).
  - Sub-pixel bilinear sampling in `EffectsEngine.kt` for clean warps without artifacts or tearing.
  - Full keyframing and curve editing for all distortion parameters.
- **Phase 7: Glitch, Pixel Sorting & Retro VHS Pack** — [COMPLETED]
  - VHS Retro Tape (`vhs_retro`): 80s/90s analog videotape tracking noise, color bleed, scanlines, and sync jitter.
  - Pixel Sorting Glitch (`pixel_sorting`): Luminance span sorting along horizontal/vertical scanline vectors with blend slider.
  - CRT Bad TV (`crt_bad_tv`): Cathode ray tube curvature, phosphor scanlines, static noise, and tube vignette.
  - Datamosh Block Glitch (`datamosh_glitch`): Video stream macroblock displacement, packet loss, and color drift.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 8: Light Leaks, Optical Flares & Atmospheric Glow Pack** — [COMPLETED]
  - Cinematic Light Leak (`light_leak`): Warm film camera drift orbs, customizable hue, speed, and radial falloff with Screen blending.
  - Anamorphic Streak Flare (`anamorphic_flare`): Highlight-thresholded horizontal/angled anamorphic optical streaks with chromatic tinting.
  - Pro Mist Diffusion (`diffusion_bloom`): Highlight diffusion halation with highlight warmth and shadow contrast preservation.
  - Volumetric God Rays (`god_rays`): Radial crepuscular light rays radiating from an adjustable emitter origin with falloff decay.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 9: Halftone, Comic, Edge Detect & Emboss Stylize Pack** — [COMPLETED]
  - Comic Halftone Screen (`halftone_dots`): Pop art/manga printed ink dot raster with dot spacing, screen angle, contrast, and color/monochrome modes.
  - Neon Edge Detect (`edge_detect`): High-pass Sobel contour extraction with glowing neon outlines, edge width, sensitivity, and source overlay switch.
  - Emboss 3D Relief (`emboss_relief`): Directional relief carving with light azimuth angle, depth elevation, and source color blending.
  - Posterize & Cel Shading (`posterize_cel`): Quantized color bands, comic ink outlines, and pop saturation boost.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 10: Cinematic Color Grading, LUTs & Tone Mapping Pack** — [COMPLETED]
  - Cinematic Teal & Orange (`teal_and_orange`): Dual-tone Hollywood grade pushing shadows to teal and highlights to amber while protecting natural skin tones.
  - Bleach Bypass Film (`bleach_bypass`): Silver retention process producing stark contrast, crushed deep blacks, and silver desaturation.
  - Retro Cross Process (`cross_process`): C-41 color reversal slide cross-process simulation with selective green/cyan shadow shifts.
  - Lift, Gamma & Gain Wheels (`color_wheels_lift_gamma_gain`): Primary 3-way color balance controls for Shadows (Lift), Midtones (Gamma), Highlights (Gain), with temperature and tint.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 11: Shimmer, Sparkle, Strobe & Edge Glow Bursts Pack** — [COMPLETED]
  - Glitter Star Sparkles (`glitter_sparkle`): Animated 4-point star sparkles blooming from highlight peaks with twinkle speed, ray size, and density.
  - Optical Strobe Pulse (`strobe_flash`): Action/club periodic optical flashes with configurable strobe frequency (0.5–20 Hz), duty cycle, and color flash mode.
  - Linear Shimmer Sweep (`shimmer_burst`): Sweeping high-gloss metallic sheen across layers with angle, streak width, sweep speed, and tint.
  - Dual-Color Rim Glow (`dual_edge_glow`): Cyberpunk dual-tone gradient rim lighting with complementary neon edge contours.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 12: Advanced Blur & Bokeh Variations Pack** — [COMPLETED]
  - Radial Zoom Blur (`zoom_blur`): Explosive focal zoom blur radiating from an interactive $(X, Y)$ center point with quality sampling.
  - Tilt-Shift Depth Blur (`tilt_shift`): Shallow miniature depth-of-field simulation with customizable linear focus position, focus band width, tilt axis angle, and gradient falloff.
  - Hexagonal Aperture Bokeh (`hexagonal_bokeh`): Optical 6-blade mechanical camera aperture bokeh blur with spherical highlight bloom and blade rotation angle.
  - Prismatic Motion Blur (`prismatic_motion_blur`): High-energy directional trajectory motion blur with RGB chromatic prism dispersion.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 13: Oscilloscope, Audio Spectrum & Visualizer Shaders Pack** — [COMPLETED]
  - Audio Oscilloscope Wave (`oscilloscope_wave`): High-voltage analog cathode ray oscilloscope waveform trace with animated multi-harmonic sine waves, beam thickness, phosphor glow pass, beam hue, and center Y baseline.
  - Frequency Spectrum Bars (`audio_spectrum_bars`): Dynamic graphic equalizer frequency bars with configurable bar count (8–64), peak height, spacing, vertical color gradient, bounce speed, baseline position, and peak caps.
  - Radial Visualizer Ring (`radial_spectrum_ring`): Circular polar audio visualizer ring pulsating radially with ray count (16–128), spike amplitude, center coordinates $(X, Y)$, and ring hue.
  - Synthwave 3D Laser Grid (`synthwave_grid`): Retro 80s outrun perspective laser floor grid moving towards horizon with perspective projection, longitudinal converging lines, quadratic latitudinal moving rungs, and neon glow.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 14: Particle Systems, Floating Dust & Ember Sparks Pack** — [COMPLETED]
  - Ambient Floating Dust Motes (`floating_dust`): Cinematic drifting micro-particles and soft bokeh motes with Brownian motion, light catching twinkle, mote count, size, speed, glow, and warmth controls.
  - Fiery Rising Ember Sparks (`rising_embers`): Organic burning embers floating upward with wind turbulence, heat shimmer, spark size, velocity, and glowing trailing tails.
  - Hyperspace Starfield Warp (`starfield_warp`): Cosmic 3D deep space starfield with radial velocity acceleration, FOV perspective, and relativistic light streak trails.
  - Atmospheric Weather Particles (`snow_rain_weather`): Natural precipitation particle physics simulating soft drifting snowflakes or angled cinematic rain streaks with wind drift.
  - Full stopwatch keyframing and cubic bezier curve control for all uniforms.
- **Phase 15: Preset Animations (Text Reveals, Pop-In, Kinetic Typography)** — [COMPLETED]
  - Animation Presets library (`PresetAnimationType`):
    - Bouncy Pop-In (Overshoot scale bounce 0% -> 120% -> 96% -> 100% with opacity fade).
    - Kinetic Slide Up & Kinetic Slide Left (smooth deceleration glide with EaseInOut).
    - Pulse Heartbeat (rhythmic musical heartbeat scale throb).
    - Action Camera Shake (high-frequency handheld camera shake with decaying amplitude).
    - Cinematic Pan & Glide (slow camera glide expanding scale with subtle drift).
    - Elastic Snap-In (snappy physics-driven entrance with spring oscillation).
    - Step Stagger Fade (crisp staircase opacity reveal).
  - Dedicated `AnimationPresetDialog` with one-tap application at the playhead timestamp.
  - Instant quick-action button in portrait and landscape Inspector panels.
- **Phase 16: Optical Flow, Speed Ramping & Time Remapping** — [COMPLETED]
  - Full layer Time Remapping architecture (`TimeRemapConfig`) with keyframe track and speed multiplier.
  - `effectiveSourceTimeMs` evaluation in `Evaluator.kt` and `FrameRenderer.kt`, remapping animation keyframes and shader playback time.
  - Speed Ramping Presets (`SpeedRampPreset`):
    - Slow-Mo Matrix (20% bullet-time playback).
    - Half Speed (50% cinematic slow-motion).
    - Hyper Speed (2x double-speed fast forward).
    - Bullet-Time Ramp (dynamic speed ramp: 250% entrance -> 25% bullet-time slow-mo -> 250% exit).
    - Reverse Playback (plays layer from end frame to start frame).
    - Freeze Frame (locks playback at current playhead frame).
  - `TimeRemapDialog` with real-time speed multiplier slider, quick pills (0.25x to 2x), and speed ramp buttons.
  - Full JSON serialization and deserialization support in `Project.kt`.
- **Phase 17: Layer Blending Modes & Track Matte Compositing** — [COMPLETED]
  - Full 12 Transfer Blend Modes (`LayerBlendMode`):
    - Normal (Standard alpha compositing).
    - Multiply (Shadows and inking).
    - Screen (Light leaks, fire, neon, blooms).
    - Overlay (Contrast enhancing).
    - Darken & Lighten (Min/Max brightness selection).
    - Color Dodge & Color Burn (High-contrast saturation shifts).
    - Hard Light (Punchy specular simulation).
    - Difference & Exclusion (Inversion and sci-fi psychedelia).
    - Add / Linear Dodge (Pure optical glow summation).
  - Track Matte System (`TrackMatteType`):
    - Alpha Matte & Alpha Inverted (using adjacent layer as stencil cutout).
    - Luma Matte & Luma Inverted (using adjacent layer luminance density as matte).
  - Live preview rendering in `PreviewCanvas` using isolated `saveLayer` and native blend modes.
  - Offline 4K export rendering in `FrameRenderer.kt`.
  - Dedicated `CompositingCard` in portrait & landscape Inspector with category pills and live indicators.
  - Visual badges on layer items in `LayersBar.kt`.
  - Full project persistence in `Project.kt`.

