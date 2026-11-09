package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.Evaluator
import com.example.engine.export.ExportConfig
import com.example.engine.export.ExportProgress
import com.example.engine.export.VideoExportPipeline
import com.example.model.Composition
import com.example.model.CubicBezier
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.Layer
import com.example.model.LayerBlendMode
import com.example.model.LayerTransformTracks
import com.example.model.LayerType
import com.example.model.PresetAnimationType
import com.example.model.Project
import com.example.model.SpeedRampPreset
import com.example.model.TrackMatteType
import com.example.model.TransformProperty
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class CanvasZoomMode(val label: String, val scale: Float) {
    FIT("Fit", 1.0f),
    ZOOM_50("50%", 0.5f),
    ZOOM_100("100%", 1.0f),
    ZOOM_200("200%", 2.0f)
}

enum class DockTab(val label: String) {
    LAYERS("Layers"),
    INSPECTOR("Inspector"),
    PROJECT("Project")
}

enum class CurveGraphMode(val label: String) {
    VALUE_GRAPH("Value Graph"),
    SPEED_GRAPH("Speed Graph")
}

data class EditorState(
    val project: Project = Project.createSampleProject(),
    val currentTimeMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedLayerId: String? = null,
    val zoomMode: CanvasZoomMode = CanvasZoomMode.FIT,
    val activeDockTab: DockTab = DockTab.LAYERS,
    val timelineZoom: Float = 1.0f,
    val showAddLayerSheet: Boolean = false,
    val showJsonSheet: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val showSmoothZoomDialog: Boolean = false,
    val showAnimationPresetDialog: Boolean = false,
    val showTimeRemapDialog: Boolean = false,
    val showAddEffectDialog: Boolean = false,
    val isCurveEditorOpen: Boolean = false,
    val isExportScreenOpen: Boolean = false,
    val curveProperty: TransformProperty? = null,
    val curveGraphMode: CurveGraphMode = CurveGraphMode.VALUE_GRAPH,
    val selectedKeyframeId: String? = null,
    val curveClipboard: CubicBezier? = null,
    val userMessage: String? = null,
    val history: List<Project> = emptyList(),
    val historyIndex: Int = 0
) {
    val canUndo: Boolean
        get() = historyIndex > 0

    val canRedo: Boolean
        get() = historyIndex < history.size - 1

    val selectedLayer: Layer?
        get() = project.layers.firstOrNull { it.id == selectedLayerId }

    val currentFrame: Int
        get() = project.composition.timeMsToFrame(currentTimeMs)

    val totalFrames: Int
        get() = project.composition.totalFrames
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        EditorState(
            selectedLayerId = Project.createSampleProject().layers.getOrNull(1)?.id
        )
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private var playbackJob: Job? = null

    val exportPipeline = VideoExportPipeline(application.applicationContext, viewModelScope)
    val exportProgress: StateFlow<ExportProgress> = exportPipeline.progress

    init {
        // Initialize history with initial project
        val initialProject = _state.value.project
        _state.update { it.copy(history = listOf(initialProject), historyIndex = 0) }
    }

    // --- Export Pipeline ---

    fun openExportScreen() {
        pause()
        _state.update { it.copy(isExportScreenOpen = true) }
    }

    fun closeExportScreen() {
        exportPipeline.cancelExport()
        _state.update { it.copy(isExportScreenOpen = false) }
    }

    fun startExport(config: ExportConfig) {
        pause()
        exportPipeline.startExport(_state.value.project, config)
    }

    fun cancelExport() {
        exportPipeline.cancelExport()
    }

    // --- Undo / Redo ---

    fun undo() {
        val curr = _state.value
        if (!curr.canUndo) return
        val newIndex = curr.historyIndex - 1
        val prevProject = curr.history[newIndex]
        _state.update {
            it.copy(
                project = prevProject,
                historyIndex = newIndex,
                userMessage = "Undo"
            )
        }
    }

    fun redo() {
        val curr = _state.value
        if (!curr.canRedo) return
        val newIndex = curr.historyIndex + 1
        val nextProject = curr.history[newIndex]
        _state.update {
            it.copy(
                project = nextProject,
                historyIndex = newIndex,
                userMessage = "Redo"
            )
        }
    }

    // --- Playback Controls ---

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        playbackJob?.cancel()
        _state.update { it.copy(isPlaying = true) }
        playbackJob = viewModelScope.launch {
            val fps = _state.value.project.composition.fps.coerceAtLeast(1)
            val frameIntervalMs = (1000L / fps).coerceAtLeast(16L)

            while (isActive) {
                delay(frameIntervalMs)
                if (!_state.value.isPlaying) break

                _state.update { curr ->
                    val nextTime = curr.currentTimeMs + frameIntervalMs
                    val duration = curr.project.composition.durationMs
                    val loopedTime = if (nextTime >= duration) 0L else nextTime
                    curr.copy(currentTimeMs = loopedTime)
                }
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _state.update { it.copy(isPlaying = false) }
    }

    fun seekTo(timeMs: Long) {
        val duration = _state.value.project.composition.durationMs
        val clamped = timeMs.coerceIn(0L, duration)
        _state.update { it.copy(currentTimeMs = clamped) }
    }

    fun stepFrame(deltaFrames: Int) {
        pause()
        _state.update { curr ->
            val comp = curr.project.composition
            val nextFrame = (comp.timeMsToFrame(curr.currentTimeMs) + deltaFrames).coerceIn(0, comp.totalFrames)
            val newTime = comp.frameToTimeMs(nextFrame)
            curr.copy(currentTimeMs = newTime)
        }
    }

    fun jumpToStart() {
        pause()
        seekTo(0L)
    }

    fun jumpToEnd() {
        pause()
        seekTo(_state.value.project.composition.durationMs)
    }

    // --- UI State & Navigation ---

    fun selectLayer(layerId: String?) {
        _state.update {
            it.copy(
                selectedLayerId = layerId,
                activeDockTab = if (layerId != null && it.activeDockTab == DockTab.PROJECT) DockTab.INSPECTOR else it.activeDockTab
            )
        }
    }

    fun setDockTab(tab: DockTab) {
        _state.update { it.copy(activeDockTab = tab) }
    }

    fun cycleZoomMode() {
        _state.update { curr ->
            val modes = CanvasZoomMode.values()
            val nextIndex = (curr.zoomMode.ordinal + 1) % modes.size
            curr.copy(zoomMode = modes[nextIndex])
        }
    }

    fun setTimelineZoom(zoomFactor: Float) {
        _state.update { it.copy(timelineZoom = zoomFactor.coerceIn(0.4f, 4.0f)) }
    }

    fun setShowAddLayer(show: Boolean) {
        _state.update { it.copy(showAddLayerSheet = show) }
    }

    fun setShowJsonSheet(show: Boolean) {
        _state.update { it.copy(showJsonSheet = show) }
    }

    fun setShowSmoothZoom(show: Boolean) {
        _state.update { it.copy(showSmoothZoomDialog = show) }
    }

    fun setShowAnimationPresets(show: Boolean) {
        _state.update { it.copy(showAnimationPresetDialog = show) }
    }

    fun setShowTimeRemap(show: Boolean) {
        _state.update { it.copy(showTimeRemapDialog = show) }
    }

    fun clearUserMessage() {
        _state.update { it.copy(userMessage = null) }
    }

    // --- Curve Editor ---

    fun openCurveEditor(layerId: String, property: TransformProperty) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val track = layer.transform.getTrack(property)
        // Ensure track is animated
        val effectiveTrack = if (!track.isAnimated || track.keyframes.isEmpty()) {
            val eval = Evaluator.evaluateTrack(track, _state.value.currentTimeMs)
            track.withKeyframe(_state.value.currentTimeMs, eval)
        } else {
            track
        }
        val updatedTransform = layer.transform.updateTrack(property, effectiveTrack)
        pushProjectMutation("Open Curve Editor", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }

        val firstKf = effectiveTrack.keyframes.firstOrNull()?.id
        _state.update {
            it.copy(
                selectedLayerId = layerId,
                curveProperty = property,
                isCurveEditorOpen = true,
                selectedKeyframeId = firstKf
            )
        }
    }

    fun closeCurveEditor() {
        _state.update { it.copy(isCurveEditorOpen = false) }
    }

    fun setCurveGraphMode(mode: CurveGraphMode) {
        _state.update { it.copy(curveGraphMode = mode) }
    }

    fun selectCurveKeyframe(keyframeId: String) {
        _state.update { it.copy(selectedKeyframeId = keyframeId) }
    }

    fun updateCurveHandle(keyframeId: String, x1: Float, y1: Float, x2: Float, y2: Float) {
        val layerId = _state.value.selectedLayerId ?: return
        val prop = _state.value.curveProperty ?: return
        val newCurve = CubicBezier(
            x1 = x1.coerceIn(0f, 1f),
            y1 = y1.coerceIn(-0.5f, 2.0f),
            x2 = x2.coerceIn(0f, 1f),
            y2 = y2.coerceIn(-0.5f, 2.0f)
        )

        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val track = layer.transform.getTrack(prop)
        val updatedTrack = track.withKeyframeCurve(keyframeId, newCurve)
        val updatedTransform = layer.transform.updateTrack(prop, updatedTrack)

        pushProjectMutation("Update Bezier Curve", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
    }

    fun commitCurveEdit() {
        val proj = _state.value.project
        pushProjectMutation("Adjust Curve Handles", recordHistory = true) { proj }
    }

    fun applyCurvePreset(keyframeId: String, preset: CubicBezier, presetName: String) {
        val layerId = _state.value.selectedLayerId ?: return
        val prop = _state.value.curveProperty ?: return
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val track = layer.transform.getTrack(prop)
        val updatedTrack = track.withKeyframeCurve(keyframeId, preset)
        val updatedTransform = layer.transform.updateTrack(prop, updatedTrack)

        pushProjectMutation("Apply $presetName curve") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
        _state.update { it.copy(userMessage = "Applied $presetName preset") }
    }

    fun copyCurve(curve: CubicBezier) {
        _state.update { it.copy(curveClipboard = curve, userMessage = "Copied curve to clipboard") }
    }

    fun pasteCurve(keyframeId: String) {
        val clip = _state.value.curveClipboard
        if (clip != null) {
            applyCurvePreset(keyframeId, clip, "Pasted")
        } else {
            _state.update { it.copy(userMessage = "Curve clipboard is empty") }
        }
    }

    // --- Smooth Zoom Quick Action ---

    fun applySmoothZoom(
        layerId: String,
        startTimeMs: Long,
        endTimeMs: Long,
        targetScalePercent: Float,
        focusX: Float,
        focusY: Float,
        enableMotionBlur: Boolean
    ) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val comp = _state.value.project.composition
        val evalStart = Evaluator.evaluateTransform(layer.transform, startTimeMs)

        // Translation calculation to center on focus point
        val currentScale = evalStart.scaleX.coerceAtLeast(10f)
        val scaleMultiplier = targetScalePercent / currentScale
        val centerX = comp.width / 2f
        val centerY = comp.height / 2f
        val deltaX = (centerX - focusX) * (scaleMultiplier - 1f) * 0.4f
        val deltaY = (centerY - focusY) * (scaleMultiplier - 1f) * 0.4f
        val targetPosX = evalStart.positionX + deltaX
        val targetPosY = evalStart.positionY + deltaY

        val easeCurve = CubicBezier.EaseInOut

        var transform = layer.transform
        val newScaleX = transform.scaleX
            .withKeyframe(startTimeMs, evalStart.scaleX, easeCurve)
            .withKeyframe(endTimeMs, targetScalePercent, easeCurve)
        val newScaleY = transform.scaleY
            .withKeyframe(startTimeMs, evalStart.scaleY, easeCurve)
            .withKeyframe(endTimeMs, targetScalePercent, easeCurve)
        val newPosX = transform.positionX
            .withKeyframe(startTimeMs, evalStart.positionX, easeCurve)
            .withKeyframe(endTimeMs, targetPosX, easeCurve)
        val newPosY = transform.positionY
            .withKeyframe(startTimeMs, evalStart.positionY, easeCurve)
            .withKeyframe(endTimeMs, targetPosY, easeCurve)

        transform = transform.copy(
            scaleX = newScaleX,
            scaleY = newScaleY,
            positionX = newPosX,
            positionY = newPosY,
            hasMotionBlur = if (enableMotionBlur) true else transform.hasMotionBlur
        )

        pushProjectMutation("Smooth Zoom Quick Action") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = transform) else it
            })
        }
        _state.update {
            it.copy(
                showSmoothZoomDialog = false,
                currentTimeMs = startTimeMs,
                userMessage = "Smooth Zoom generated (${targetScalePercent.toInt()}%)"
            )
        }
    }

    // --- Phase 15: Preset Animations ---

    fun applyAnimationPreset(layerId: String, preset: PresetAnimationType, startTimeMs: Long) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val eval = Evaluator.evaluateTransform(layer.transform, startTimeMs)

        val newTransform = preset.applyToTransform(
            currentTransform = layer.transform,
            startTimeMs = startTimeMs,
            baseX = eval.positionX,
            baseY = eval.positionY,
            baseScaleX = eval.scaleX,
            baseScaleY = eval.scaleY
        )

        pushProjectMutation("Apply Animation: ${preset.title}") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = newTransform) else it
            })
        }

        _state.update {
            it.copy(
                showAnimationPresetDialog = false,
                currentTimeMs = startTimeMs,
                userMessage = "Preset '${preset.title}' applied"
            )
        }
    }

    // --- Phase 16: Speed Ramping & Time Remapping ---

    fun toggleTimeRemap(layerId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val newEnabled = !layer.timeRemap.isEnabled

        val defaultTrack = if (newEnabled && layer.timeRemap.track.keyframes.isEmpty()) {
            layer.timeRemap.track
                .withKeyframe(layer.startTimeMs, 0f, CubicBezier.Linear)
                .withKeyframe(layer.endTimeMs, layer.durationMs.toFloat(), CubicBezier.Linear)
        } else layer.timeRemap.track

        val newConfig = layer.timeRemap.copy(
            isEnabled = newEnabled,
            track = defaultTrack
        )

        pushProjectMutation("Toggle Time Remap") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(timeRemap = newConfig) else it
            })
        }

        _state.update {
            it.copy(userMessage = if (newEnabled) "Time Remapping enabled" else "Time Remapping disabled")
        }
    }

    fun setTimeRemapMultiplier(layerId: String, multiplier: Float) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val clamped = multiplier.coerceIn(0.1f, 10f)
        val newConfig = layer.timeRemap.copy(isEnabled = true, speedMultiplier = clamped)

        pushProjectMutation("Set Speed ${String.format("%.1fx", clamped)}") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(timeRemap = newConfig) else it
            })
        }
    }

    fun applySpeedRampPreset(layerId: String, preset: SpeedRampPreset) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val startMs = layer.startTimeMs
        val durMs = layer.durationMs
        val endMs = layer.endTimeMs

        val newConfig = when (preset) {
            SpeedRampPreset.SLOW_MO_20 -> {
                layer.timeRemap.copy(
                    isEnabled = true,
                    speedMultiplier = 0.2f,
                    track = layer.timeRemap.track
                        .withKeyframe(startMs, 0f, CubicBezier.Linear)
                        .withKeyframe(endMs, durMs * 0.2f, CubicBezier.Linear)
                )
            }
            SpeedRampPreset.SLOW_MO_50 -> {
                layer.timeRemap.copy(
                    isEnabled = true,
                    speedMultiplier = 0.5f,
                    track = layer.timeRemap.track
                        .withKeyframe(startMs, 0f, CubicBezier.Linear)
                        .withKeyframe(endMs, durMs * 0.5f, CubicBezier.Linear)
                )
            }
            SpeedRampPreset.FAST_FORWARD_200 -> {
                layer.timeRemap.copy(
                    isEnabled = true,
                    speedMultiplier = 2.0f,
                    track = layer.timeRemap.track
                        .withKeyframe(startMs, 0f, CubicBezier.Linear)
                        .withKeyframe(endMs, durMs * 2.0f, CubicBezier.Linear)
                )
            }
            SpeedRampPreset.FAST_SLOW_FAST -> {
                val t1 = startMs + (durMs * 0.30f).toLong()
                val t2 = startMs + (durMs * 0.70f).toLong()
                val s1 = durMs * 0.50f
                val s2 = durMs * 0.65f

                val track = KeyframeTrack(
                    isAnimated = true,
                    defaultValue = 0f,
                    keyframes = listOf(
                        Keyframe(timeMs = startMs, value = 0f, curveToNext = CubicBezier.EaseInOut),
                        Keyframe(timeMs = t1, value = s1, curveToNext = CubicBezier.EaseInOut),
                        Keyframe(timeMs = t2, value = s2, curveToNext = CubicBezier.EaseInOut),
                        Keyframe(timeMs = endMs, value = durMs.toFloat(), curveToNext = CubicBezier.EaseInOut)
                    )
                )
                layer.timeRemap.copy(isEnabled = true, track = track)
            }
            SpeedRampPreset.REVERSE -> {
                val track = KeyframeTrack(
                    isAnimated = true,
                    defaultValue = durMs.toFloat(),
                    keyframes = listOf(
                        Keyframe(timeMs = startMs, value = durMs.toFloat(), curveToNext = CubicBezier.Linear),
                        Keyframe(timeMs = endMs, value = 0f, curveToNext = CubicBezier.Linear)
                    )
                )
                layer.timeRemap.copy(isEnabled = true, track = track)
            }
            SpeedRampPreset.FREEZE_FRAME -> {
                val currentCompTime = _state.value.currentTimeMs
                val freezeSourceTime = layer.effectiveSourceTimeMs(currentCompTime).toFloat()
                val track = KeyframeTrack(
                    isAnimated = true,
                    defaultValue = freezeSourceTime,
                    keyframes = listOf(
                        Keyframe(timeMs = startMs, value = freezeSourceTime, curveToNext = CubicBezier.Linear),
                        Keyframe(timeMs = endMs, value = freezeSourceTime, curveToNext = CubicBezier.Linear)
                    )
                )
                layer.timeRemap.copy(isEnabled = true, track = track)
            }
        }

        pushProjectMutation("Apply Speed Preset: ${preset.label}") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(timeRemap = newConfig) else it
            })
        }

        _state.update {
            it.copy(
                showTimeRemapDialog = false,
                userMessage = "Speed Ramp: ${preset.label} applied"
            )
        }
    }

    // --- Layer Operations ---

    fun addLayer(type: LayerType, name: String? = null) {
        val defaultName = name ?: when (type) {
            is LayerType.Text -> "Text ${type.text.take(8)}"
            is LayerType.ShapeRect -> "Rectangle"
            is LayerType.ShapeCircle -> "Circle"
            is LayerType.SolidColor -> "Solid Backdrop"
            is LayerType.Media -> "Media Clip"
            is LayerType.Adjustment -> "Adjustment"
            is LayerType.Group -> "Group"
        }

        val comp = _state.value.project.composition
        val newLayer = Layer(
            id = UUID.randomUUID().toString(),
            name = defaultName,
            type = type,
            startTimeMs = _state.value.currentTimeMs,
            durationMs = (comp.durationMs - _state.value.currentTimeMs).coerceAtLeast(1000L),
            transform = LayerTransformTracks(
                positionX = KeyframeTrack(defaultValue = comp.width / 2f),
                positionY = KeyframeTrack(defaultValue = comp.height / 2f)
            )
        )

        pushProjectMutation("Add Layer $defaultName") { currProj ->
            currProj.copy(layers = currProj.layers + newLayer)
        }

        _state.update {
            it.copy(
                selectedLayerId = newLayer.id,
                activeDockTab = DockTab.INSPECTOR,
                showAddLayerSheet = false,
                userMessage = "Layer '$defaultName' created"
            )
        }
    }

    fun deleteLayer(layerId: String) {
        val layerName = _state.value.project.layers.firstOrNull { it.id == layerId }?.name ?: "Layer"
        pushProjectMutation("Delete Layer $layerName") { currProj ->
            currProj.copy(layers = currProj.layers.filterNot { it.id == layerId })
        }
        _state.update {
            it.copy(
                selectedLayerId = if (it.selectedLayerId == layerId) null else it.selectedLayerId,
                userMessage = "Deleted $layerName"
            )
        }
    }

    fun duplicateLayer(layerId: String) {
        val original = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val duplicate = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} Copy",
            transform = original.transform.copy(
                positionX = original.transform.positionX.copy(defaultValue = original.transform.positionX.defaultValue + 40f),
                positionY = original.transform.positionY.copy(defaultValue = original.transform.positionY.defaultValue + 40f)
            )
        )
        pushProjectMutation("Duplicate Layer ${original.name}") { currProj ->
            val index = currProj.layers.indexOfFirst { it.id == layerId }
            val mutable = currProj.layers.toMutableList()
            if (index >= 0) {
                mutable.add(index + 1, duplicate)
            } else {
                mutable.add(duplicate)
            }
            currProj.copy(layers = mutable)
        }
        _state.update { it.copy(selectedLayerId = duplicate.id, userMessage = "Duplicated ${original.name}") }
    }

    fun reorderLayer(fromIndex: Int, toIndex: Int) {
        val layers = _state.value.project.layers
        if (fromIndex !in layers.indices || toIndex !in layers.indices) return
        pushProjectMutation("Reorder Layer") { currProj ->
            val mutable = currProj.layers.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            currProj.copy(layers = mutable)
        }
    }

    fun toggleVisibility(layerId: String) {
        pushProjectMutation("Toggle Visibility") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            })
        }
    }

    fun toggleLock(layerId: String) {
        pushProjectMutation("Toggle Lock") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
            })
        }
    }

    fun updateLayerTiming(layerId: String, startTimeMs: Long, durationMs: Long) {
        val comp = _state.value.project.composition
        val validStart = startTimeMs.coerceIn(0L, comp.durationMs - 100L)
        val validDuration = durationMs.coerceIn(100L, comp.durationMs - validStart)

        pushProjectMutation("Trim Layer") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(startTimeMs = validStart, durationMs = validDuration) else it
            })
        }
    }

    fun renameLayer(layerId: String, newName: String) {
        pushProjectMutation("Rename Layer") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(name = newName) else it
            })
        }
    }

    fun setLayerBlendMode(layerId: String, blendMode: LayerBlendMode) {
        pushProjectMutation("Set Blend Mode: ${blendMode.label}") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(blendMode = blendMode) else it
            })
        }
        _state.update { it.copy(userMessage = "Blend Mode: ${blendMode.label}") }
    }

    fun setLayerTrackMatte(layerId: String, trackMatte: TrackMatteType) {
        pushProjectMutation("Set Track Matte: ${trackMatte.label}") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(trackMatte = trackMatte) else it
            })
        }
        _state.update { it.copy(userMessage = "Track Matte: ${trackMatte.label}") }
    }

    // --- Phase 4: Background Removal Actions ---

    fun toggleBackgroundRemoval(layerId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val mediaType = layer.type as? LayerType.Media ?: return
        val currentBg = mediaType.bgRemoval
        val newBg = currentBg.copy(isEnabled = !currentBg.isEnabled)
        val updatedLayer = layer.copy(type = mediaType.copy(bgRemoval = newBg))

        pushProjectMutation(if (newBg.isEnabled) "Enable Background Removal" else "Disable Background Removal") { currProj ->
            currProj.copy(layers = currProj.layers.map { if (it.id == layerId) updatedLayer else it })
        }
        _state.update {
            it.copy(userMessage = if (newBg.isEnabled) "Remove Background: ON" else "Remove Background: OFF")
        }
    }

    fun updateBgRemovalFeather(layerId: String, feather: Float) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val mediaType = layer.type as? LayerType.Media ?: return
        val updatedBg = mediaType.bgRemoval.copy(edgeFeather = feather.coerceIn(0f, 40f))
        val updatedLayer = layer.copy(type = mediaType.copy(bgRemoval = updatedBg))

        pushProjectMutation("Update Edge Feather", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map { if (it.id == layerId) updatedLayer else it })
        }
    }

    fun updateBgRemovalShrinkGrow(layerId: String, shrinkGrow: Float) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val mediaType = layer.type as? LayerType.Media ?: return
        val updatedBg = mediaType.bgRemoval.copy(shrinkGrow = shrinkGrow.coerceIn(-30f, 30f))
        val updatedLayer = layer.copy(type = mediaType.copy(bgRemoval = updatedBg))

        pushProjectMutation("Update Shrink/Grow", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map { if (it.id == layerId) updatedLayer else it })
        }
    }

    fun toggleBgRemovalPreviewMask(layerId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val mediaType = layer.type as? LayerType.Media ?: return
        val updatedBg = mediaType.bgRemoval.copy(previewMask = !mediaType.bgRemoval.previewMask)
        val updatedLayer = layer.copy(type = mediaType.copy(bgRemoval = updatedBg))

        pushProjectMutation("Toggle Preview Mask", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map { if (it.id == layerId) updatedLayer else it })
        }
    }

    fun updateBgRemovalThreshold(layerId: String, threshold: Float) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val mediaType = layer.type as? LayerType.Media ?: return
        val updatedBg = mediaType.bgRemoval.copy(threshold = threshold.coerceIn(0.1f, 0.9f))
        val updatedLayer = layer.copy(type = mediaType.copy(bgRemoval = updatedBg))

        pushProjectMutation("Update Threshold", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map { if (it.id == layerId) updatedLayer else it })
        }
    }

    fun clearSegmentationCache() {
        com.example.engine.segmentation.SegmentationEngine.clearCache()
        _state.update { it.copy(userMessage = "Mask Cache Cleared") }
    }

    // --- Phase 5: Effects Engine Actions ---

    fun setShowAddEffectDialog(show: Boolean) {
        _state.update { it.copy(showAddEffectDialog = show) }
    }

    fun addEffectToLayer(layerId: String, effectDefId: String) {
        val def = com.example.model.effect.EffectsRegistry.getEffectById(effectDefId) ?: return
        val defaultTracks = mutableMapOf<String, KeyframeTrack<Float>>()
        for (p in def.parameters) {
            defaultTracks[p.id] = KeyframeTrack(defaultValue = p.defaultVal)
        }
        val newEffect = com.example.model.effect.AppliedEffect(
            effectDefId = effectDefId,
            parameterTracks = defaultTracks
        )

        pushProjectMutation("Add Effect ${def.name}") { currProj ->
            currProj.copy(layers = currProj.layers.map { layer ->
                if (layer.id == layerId) layer.copy(effects = layer.effects + newEffect) else layer
            })
        }
        _state.update { it.copy(userMessage = "Added ${def.name} to layer") }
    }

    fun removeEffectFromLayer(layerId: String, effectId: String) {
        pushProjectMutation("Remove Effect") { currProj ->
            currProj.copy(layers = currProj.layers.map { layer ->
                if (layer.id == layerId) layer.copy(effects = layer.effects.filterNot { it.id == effectId }) else layer
            })
        }
        _state.update { it.copy(userMessage = "Effect removed") }
    }

    fun toggleEffectBypass(layerId: String, effectId: String) {
        pushProjectMutation("Toggle Effect Bypass") { currProj ->
            currProj.copy(layers = currProj.layers.map { layer ->
                if (layer.id == layerId) {
                    layer.copy(effects = layer.effects.map { eff ->
                        if (eff.id == effectId) eff.copy(isEnabled = !eff.isEnabled) else eff
                    })
                } else layer
            })
        }
    }

    fun reorderEffect(layerId: String, fromIndex: Int, toIndex: Int) {
        pushProjectMutation("Reorder Effect") { currProj ->
            currProj.copy(layers = currProj.layers.map { layer ->
                if (layer.id == layerId && fromIndex in layer.effects.indices && toIndex in layer.effects.indices) {
                    val mutable = layer.effects.toMutableList()
                    val moved = mutable.removeAt(fromIndex)
                    mutable.add(toIndex, moved)
                    layer.copy(effects = mutable)
                } else layer
            })
        }
    }

    fun updateEffectParamValue(layerId: String, effectId: String, paramId: String, value: Float) {
        val currentTime = _state.value.currentTimeMs
        pushProjectMutation("Update Effect Parameter", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map { layer ->
                if (layer.id == layerId) {
                    layer.copy(effects = layer.effects.map { eff ->
                        if (eff.id == effectId) {
                            val track = eff.parameterTracks[paramId] ?: KeyframeTrack(defaultValue = value)
                            val updatedTrack = if (track.isAnimated) {
                                track.withKeyframe(currentTime, value)
                            } else {
                                track.copy(defaultValue = value)
                            }
                            eff.copy(parameterTracks = eff.parameterTracks + (paramId to updatedTrack))
                        } else eff
                    })
                } else layer
            })
        }
    }

    fun toggleEffectParamStopwatch(layerId: String, effectId: String, paramId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val effect = layer.effects.firstOrNull { it.id == effectId } ?: return
        val def = com.example.model.effect.EffectsRegistry.getEffectById(effect.effectDefId) ?: return
        val pDef = def.parameters.firstOrNull { it.id == paramId } ?: return
        val track = effect.parameterTracks[paramId] ?: KeyframeTrack(defaultValue = pDef.defaultVal)
        val currentTime = _state.value.currentTimeMs
        val currentVal = Evaluator.evaluateTrack(track, currentTime)

        val updatedTrack = if (track.isAnimated) {
            track.copy(isAnimated = false, defaultValue = currentVal, keyframes = emptyList())
        } else {
            track.withKeyframe(currentTime, currentVal)
        }

        pushProjectMutation("Toggle Stopwatch ${pDef.label}") { currProj ->
            currProj.copy(layers = currProj.layers.map { l ->
                if (l.id == layerId) {
                    l.copy(effects = l.effects.map { eff ->
                        if (eff.id == effectId) {
                            eff.copy(parameterTracks = eff.parameterTracks + (paramId to updatedTrack))
                        } else eff
                    })
                } else l
            })
        }
    }

    // --- Transform & Keyframe Operations ---

    fun toggleStopwatch(layerId: String, prop: TransformProperty) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val currentTrack = layer.transform.getTrack(prop)
        val currentTime = _state.value.currentTimeMs
        val evaluatedTransform = Evaluator.evaluateTransform(layer.transform, currentTime)
        val currentEvaluatedValue = when (prop) {
            TransformProperty.POSITION_X -> evaluatedTransform.positionX
            TransformProperty.POSITION_Y -> evaluatedTransform.positionY
            TransformProperty.SCALE_X -> evaluatedTransform.scaleX
            TransformProperty.SCALE_Y -> evaluatedTransform.scaleY
            TransformProperty.PIVOT_X -> evaluatedTransform.pivotX
            TransformProperty.PIVOT_Y -> evaluatedTransform.pivotY
            TransformProperty.ROTATION -> evaluatedTransform.rotation
            TransformProperty.OPACITY -> evaluatedTransform.opacity
        }

        val updatedTrack = if (currentTrack.isAnimated) {
            // Turning stopwatch OFF: keep current evaluated value as default, clear keyframes
            currentTrack.copy(
                isAnimated = false,
                defaultValue = currentEvaluatedValue,
                keyframes = emptyList()
            )
        } else {
            // Turning stopwatch ON: insert first keyframe at current playhead
            currentTrack.copy(
                isAnimated = true,
                defaultValue = currentEvaluatedValue,
                keyframes = listOf(Keyframe(timeMs = currentTime, value = currentEvaluatedValue))
            )
        }

        val updatedTransform = layer.transform.updateTrack(prop, updatedTrack)
        pushProjectMutation("Toggle Stopwatch for ${prop.label}") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
    }

    fun updatePropertyValue(layerId: String, prop: TransformProperty, newValue: Float) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val currentTrack = layer.transform.getTrack(prop)
        val currentTime = _state.value.currentTimeMs

        val updatedTrack = if (currentTrack.isAnimated) {
            currentTrack.withKeyframe(currentTime, newValue)
        } else {
            currentTrack.copy(defaultValue = newValue)
        }

        val updatedTransform = layer.transform.updateTrack(prop, updatedTrack)
        pushProjectMutation("Update ${prop.label}", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
    }

    fun deleteKeyframeAtPlayhead(layerId: String, prop: TransformProperty) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val track = layer.transform.getTrack(prop)
        val kf = track.findKeyframeAt(_state.value.currentTimeMs) ?: return

        val updatedTrack = track.withoutKeyframe(kf.id)
        val updatedTransform = layer.transform.updateTrack(prop, updatedTrack)
        pushProjectMutation("Delete Keyframe") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
    }

    fun jumpToPreviousKeyframe(layerId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val times = layer.transform.allKeyframeTimes.filter { it < _state.value.currentTimeMs - 15L }
        if (times.isNotEmpty()) {
            seekTo(times.maxOrNull() ?: 0L)
        }
    }

    fun jumpToNextKeyframe(layerId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val times = layer.transform.allKeyframeTimes.filter { it > _state.value.currentTimeMs + 15L }
        if (times.isNotEmpty()) {
            seekTo(times.minOrNull() ?: _state.value.currentTimeMs)
        }
    }

    fun toggleScaleLink(layerId: String) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        val updatedTransform = layer.transform.copy(scaleLinked = !layer.transform.scaleLinked)
        pushProjectMutation("Toggle Scale Link") { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
    }

    // Direct canvas translation
    fun dragTranslateLayer(layerId: String, deltaX: Float, deltaY: Float) {
        val layer = _state.value.project.layers.firstOrNull { it.id == layerId } ?: return
        if (layer.isLocked) return

        val eval = Evaluator.evaluateTransform(layer.transform, _state.value.currentTimeMs)
        val newX = eval.positionX + deltaX
        val newY = eval.positionY + deltaY

        val trackX = layer.transform.positionX
        val trackY = layer.transform.positionY
        val currentTime = _state.value.currentTimeMs

        val updatedTrackX = if (trackX.isAnimated) trackX.withKeyframe(currentTime, newX) else trackX.copy(defaultValue = newX)
        val updatedTrackY = if (trackY.isAnimated) trackY.withKeyframe(currentTime, newY) else trackY.copy(defaultValue = newY)

        val updatedTransform = layer.transform.copy(positionX = updatedTrackX, positionY = updatedTrackY)
        pushProjectMutation("Drag Move", recordHistory = false) { currProj ->
            currProj.copy(layers = currProj.layers.map {
                if (it.id == layerId) it.copy(transform = updatedTransform) else it
            })
        }
    }

    // --- JSON Save & Load ---

    fun getProjectJson(): String {
        return _state.value.project.toJsonString(indentSpaces = 2)
    }

    fun loadProjectFromJson(jsonString: String): Boolean {
        return try {
            val project = Project.fromJsonString(jsonString)
            _state.update {
                it.copy(
                    project = project,
                    currentTimeMs = 0L,
                    selectedLayerId = project.layers.firstOrNull()?.id,
                    userMessage = "Project '${project.name}' loaded successfully!"
                )
            }
            true
        } catch (e: Exception) {
            _state.update { it.copy(userMessage = "Failed to parse JSON: ${e.message}") }
            false
        }
    }

    fun resetToSampleProject() {
        val sample = Project.createSampleProject()
        _state.update {
            it.copy(
                project = sample,
                currentTimeMs = 0L,
                selectedLayerId = sample.layers.getOrNull(1)?.id,
                userMessage = "Loaded sample Cyberpunk Intro project"
            )
        }
    }

    fun newEmptyProject() {
        val emptyProj = Project(
            name = "New Composition",
            composition = Composition(),
            layers = listOf(
                Layer(
                    name = "Solid Dark",
                    type = LayerType.SolidColor(fillColorHex = "#10121A")
                )
            )
        )
        _state.update {
            it.copy(
                project = emptyProj,
                currentTimeMs = 0L,
                selectedLayerId = emptyProj.layers.firstOrNull()?.id,
                userMessage = "Created new project"
            )
        }
    }

    // --- History / Mutation helper ---

    fun pushProjectMutation(
        description: String,
        recordHistory: Boolean = true,
        mutator: (Project) -> Project
    ) {
        _state.update { curr ->
            val updatedProject = mutator(curr.project)
            if (recordHistory) {
                val newHistory = curr.history.take(curr.historyIndex + 1) + updatedProject
                curr.copy(
                    project = updatedProject,
                    history = newHistory.takeLast(40),
                    historyIndex = (newHistory.size - 1).coerceAtMost(39)
                )
            } else {
                curr.copy(project = updatedProject)
            }
        }
    }
}
