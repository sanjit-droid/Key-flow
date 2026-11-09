package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddLayerDialog
import com.example.ui.components.CurveEditorScreen
import com.example.ui.components.ExportScreen
import com.example.ui.components.effects.AddEffectDialog
import com.example.ui.dialog.AnimationPresetDialog
import com.example.ui.dialog.TimeRemapDialog
import com.example.ui.components.InspectorView
import com.example.ui.components.LandscapeInspectorView
import com.example.ui.components.LandscapeWorkstationTimeline
import com.example.ui.components.LayersBar
import com.example.ui.components.PreviewCanvas
import com.example.ui.components.ProjectJsonDialog
import com.example.ui.components.SmoothZoomDialog
import com.example.ui.components.TimelineView
import com.example.ui.components.TransportBar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.DockTab
import com.example.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                // ==================== LANDSCAPE WORKSTATION LAYOUT (MATCHING SCREENSHOT) ====================
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Panel: Screenshot-styled Inspector (Width 270dp)
                    LandscapeInspectorView(
                        state = state,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onToggleStopwatch = { id, prop -> viewModel.toggleStopwatch(id, prop) },
                        onUpdatePropertyValue = { id, prop, v -> viewModel.updatePropertyValue(id, prop, v) },
                        onToggleScaleLink = { id -> viewModel.toggleScaleLink(id) },
                        onOpenCurveEditor = { id, prop -> viewModel.openCurveEditor(id, prop) },
                        onOpenSmoothZoom = { viewModel.setShowSmoothZoom(true) },
                        onAddEffectClick = { viewModel.setShowAddLayer(true) },
                        onToggleBackgroundRemoval = { id -> viewModel.toggleBackgroundRemoval(id) },
                        onUpdateBgRemovalFeather = { id, f -> viewModel.updateBgRemovalFeather(id, f) },
                        onUpdateBgRemovalShrinkGrow = { id, sg -> viewModel.updateBgRemovalShrinkGrow(id, sg) },
                        onToggleBgRemovalPreviewMask = { id -> viewModel.toggleBgRemovalPreviewMask(id) },
                        onUpdateBgRemovalThreshold = { id, th -> viewModel.updateBgRemovalThreshold(id, th) },
                        onClearBgRemovalCache = { viewModel.clearSegmentationCache() },
                        onOpenAddEffectDialog = { viewModel.setShowAddEffectDialog(true) },
                        onToggleEffectBypass = { lId, effId -> viewModel.toggleEffectBypass(lId, effId) },
                        onDeleteEffect = { lId, effId -> viewModel.removeEffectFromLayer(lId, effId) },
                        onReorderEffect = { lId, from, to -> viewModel.reorderEffect(lId, from, to) },
                        onUpdateEffectParamValue = { lId, effId, pId, v -> viewModel.updateEffectParamValue(lId, effId, pId, v) },
                        onToggleEffectParamStopwatch = { lId, effId, pId -> viewModel.toggleEffectParamStopwatch(lId, effId, pId) },
                        onOpenEffectCurveEditor = { _, _, _ -> },
                        onOpenAnimationPresets = { viewModel.setShowAnimationPresets(true) },
                        onOpenTimeRemap = { viewModel.setShowTimeRemap(true) },
                        onSetBlendMode = { id, mode -> viewModel.setLayerBlendMode(id, mode) },
                        onSetTrackMatte = { id, matte -> viewModel.setLayerTrackMatte(id, matte) }
                    )

                    // Right Area: Canvas on top, then Screenshot-matched Timeline & Transport on bottom
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Preview Canvas with Viewport zoom toggle [ + ]
                        PreviewCanvas(
                            state = state,
                            onDragLayer = { id, dx, dy -> viewModel.dragTranslateLayer(id, dx, dy) },
                            onCycleZoom = { viewModel.cycleZoomMode() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        // Landscape Workstation Timeline & Transport Bar (Hot pink bar, Green track, Ruler, Transport)
                        LandscapeWorkstationTimeline(
                            state = state,
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            onSelectLayer = { viewModel.selectLayer(it) },
                            onAddLayerClick = { viewModel.setShowAddLayer(true) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onStepFrame = { viewModel.stepFrame(it) },
                            onJumpStart = { viewModel.jumpToStart() },
                            onJumpEnd = { viewModel.jumpToEnd() },
                            onSeek = { viewModel.seekTo(it) },
                            onUpdateLayerTiming = { id, start, dur -> viewModel.updateLayerTiming(id, start, dur) },
                            onExportClick = { viewModel.openExportScreen() }
                        )
                    }
                }
            } else {
                // ==================== PORTRAIT MOBILE LAYOUT ====================
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Top App Bar (Branding, Undo/Redo, Export, Project JSON, Add Layer)
                    TopEditorBar(
                        projectName = state.project.name,
                        resolutionText = "${state.project.composition.width}x${state.project.composition.height}",
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onOpenExport = { viewModel.openExportScreen() },
                        onOpenProject = { viewModel.setShowJsonSheet(true) },
                        onAddLayer = { viewModel.setShowAddLayer(true) }
                    )

                    // 2. Preview Canvas Area (Viewport with Fit/Zoom & Gesture Translation)
                    PreviewCanvas(
                        state = state,
                        onDragLayer = { id, dx, dy -> viewModel.dragTranslateLayer(id, dx, dy) },
                        onCycleZoom = { viewModel.cycleZoomMode() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.05f)
                    )

                    // 3. Transport Controls Bar (Play/Pause, Step Frames, Timecode, Scrub Slider)
                    TransportBar(
                        state = state,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onStepFrame = { viewModel.stepFrame(it) },
                        onJumpStart = { viewModel.jumpToStart() },
                        onJumpEnd = { viewModel.jumpToEnd() },
                        onSeek = { viewModel.seekTo(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 4. Timeline Section (Ruler, Trimmable Tracks, Keyframe Markers, Playhead)
                    TimelineView(
                        state = state,
                        onSeek = { viewModel.seekTo(it) },
                        onSelectLayer = { viewModel.selectLayer(it) },
                        onUpdateLayerTiming = { id, start, dur -> viewModel.updateLayerTiming(id, start, dur) },
                        onZoomChange = { viewModel.setTimelineZoom(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    // 5. Dock Tabs (Layers / Inspector)
                    DockTabBar(
                        currentTab = state.activeDockTab,
                        layersCount = state.project.layers.size,
                        selectedLayerName = state.selectedLayer?.name,
                        onSelectTab = { viewModel.setDockTab(it) }
                    )

                    // 6. Dock Content Panel (Layers list or Transform Inspector)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.15f)
                    ) {
                        when (state.activeDockTab) {
                            DockTab.LAYERS -> {
                                LayersBar(
                                    state = state,
                                    onSelectLayer = { viewModel.selectLayer(it) },
                                    onAddLayerClick = { viewModel.setShowAddLayer(true) },
                                    onToggleVisibility = { viewModel.toggleVisibility(it) },
                                    onToggleLock = { viewModel.toggleLock(it) },
                                    onDeleteLayer = { viewModel.deleteLayer(it) },
                                    onDuplicateLayer = { viewModel.duplicateLayer(it) },
                                    onReorder = { from, to -> viewModel.reorderLayer(from, to) }
                                )
                            }

                            DockTab.INSPECTOR -> {
                                InspectorView(
                                    state = state,
                                    onToggleStopwatch = { id, prop -> viewModel.toggleStopwatch(id, prop) },
                                    onUpdatePropertyValue = { id, prop, v -> viewModel.updatePropertyValue(id, prop, v) },
                                    onDeleteKeyframeAtPlayhead = { id, prop -> viewModel.deleteKeyframeAtPlayhead(id, prop) },
                                    onToggleScaleLink = { id -> viewModel.toggleScaleLink(id) },
                                    onJumpPrevKf = { id -> viewModel.jumpToPreviousKeyframe(id) },
                                    onJumpNextKf = { id -> viewModel.jumpToNextKeyframe(id) },
                                    onRenameLayer = { id, name -> viewModel.renameLayer(id, name) },
                                    onOpenCurveEditor = { id, prop -> viewModel.openCurveEditor(id, prop) },
                                    onOpenSmoothZoom = { viewModel.setShowSmoothZoom(true) },
                                    onToggleBackgroundRemoval = { id -> viewModel.toggleBackgroundRemoval(id) },
                                    onUpdateBgRemovalFeather = { id, f -> viewModel.updateBgRemovalFeather(id, f) },
                                    onUpdateBgRemovalShrinkGrow = { id, sg -> viewModel.updateBgRemovalShrinkGrow(id, sg) },
                                    onToggleBgRemovalPreviewMask = { id -> viewModel.toggleBgRemovalPreviewMask(id) },
                                    onUpdateBgRemovalThreshold = { id, th -> viewModel.updateBgRemovalThreshold(id, th) },
                                    onClearBgRemovalCache = { viewModel.clearSegmentationCache() },
                                    onOpenAddEffectDialog = { viewModel.setShowAddEffectDialog(true) },
                                    onToggleEffectBypass = { lId, effId -> viewModel.toggleEffectBypass(lId, effId) },
                                    onDeleteEffect = { lId, effId -> viewModel.removeEffectFromLayer(lId, effId) },
                                    onReorderEffect = { lId, from, to -> viewModel.reorderEffect(lId, from, to) },
                                    onUpdateEffectParamValue = { lId, effId, pId, v -> viewModel.updateEffectParamValue(lId, effId, pId, v) },
                                    onToggleEffectParamStopwatch = { lId, effId, pId -> viewModel.toggleEffectParamStopwatch(lId, effId, pId) },
                                    onOpenEffectCurveEditor = { _, _, _ -> },
                                    onOpenAnimationPresets = { viewModel.setShowAnimationPresets(true) },
                                    onOpenTimeRemap = { viewModel.setShowTimeRemap(true) },
                                    onSetBlendMode = { id, mode -> viewModel.setLayerBlendMode(id, mode) },
                                    onSetTrackMatte = { id, matte -> viewModel.setLayerTrackMatte(id, matte) }
                                )
                            }

                            DockTab.PROJECT -> {
                                viewModel.setShowJsonSheet(true)
                                viewModel.setDockTab(DockTab.LAYERS)
                            }
                        }
                    }
                }
            }

            // Curve Editor Overlay Screen
            if (state.isCurveEditorOpen) {
                CurveEditorScreen(
                    state = state,
                    onClose = { viewModel.closeCurveEditor() },
                    onSetGraphMode = { viewModel.setCurveGraphMode(it) },
                    onSelectKeyframe = { viewModel.selectCurveKeyframe(it) },
                    onUpdateCurveHandle = { kfId, x1, y1, x2, y2 ->
                        viewModel.updateCurveHandle(kfId, x1, y1, x2, y2)
                    },
                    onCommitCurve = { viewModel.commitCurveEdit() },
                    onApplyPreset = { kfId, preset, name ->
                        viewModel.applyCurvePreset(kfId, preset, name)
                    },
                    onCopyCurve = { viewModel.copyCurve(it) },
                    onPasteCurve = { kfId -> viewModel.pasteCurve(kfId) },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
                )
            }

            // Export Screen Overlay
            if (state.isExportScreenOpen) {
                ExportScreen(
                    project = state.project,
                    progress = exportProgress,
                    onStartExport = { config -> viewModel.startExport(config) },
                    onCancelExport = { viewModel.cancelExport() },
                    onClose = { viewModel.closeExportScreen() }
                )
            }
        }

        // Add Layer Dialog
        if (state.showAddLayerSheet) {
            AddLayerDialog(
                onDismiss = { viewModel.setShowAddLayer(false) },
                onAddLayer = { type -> viewModel.addLayer(type) }
            )
        }

        // Project JSON Dialog
        if (state.showJsonSheet) {
            ProjectJsonDialog(
                projectJson = viewModel.getProjectJson(),
                onImportJson = { viewModel.loadProjectFromJson(it) },
                onResetSample = { viewModel.resetToSampleProject() },
                onNewProject = { viewModel.newEmptyProject() },
                onDismiss = { viewModel.setShowJsonSheet(false) }
            )
        }

        // Smooth Zoom Quick Action Dialog
        if (state.showSmoothZoomDialog && state.selectedLayer != null) {
            SmoothZoomDialog(
                layer = state.selectedLayer!!,
                composition = state.project.composition,
                currentPlayheadMs = state.currentTimeMs,
                onDismiss = { viewModel.setShowSmoothZoom(false) },
                onApplySmoothZoom = { start, end, scale, fx, fy, blur ->
                    viewModel.applySmoothZoom(
                        layerId = state.selectedLayer!!.id,
                        startTimeMs = start,
                        endTimeMs = end,
                        targetScalePercent = scale,
                        focusX = fx,
                        focusY = fy,
                        enableMotionBlur = blur
                    )
                }
            )
        }

        // Phase 5: Add Effect Browser Dialog
        if (state.showAddEffectDialog && state.selectedLayer != null) {
            AddEffectDialog(
                onDismiss = { viewModel.setShowAddEffectDialog(false) },
                onAddEffect = { effectDefId ->
                    viewModel.addEffectToLayer(state.selectedLayer!!.id, effectDefId)
                }
            )
        }

        // Phase 15: Animation Preset Dialog
        if (state.showAnimationPresetDialog && state.selectedLayer != null) {
            AnimationPresetDialog(
                layer = state.selectedLayer!!,
                currentTimeMs = state.currentTimeMs,
                onDismiss = { viewModel.setShowAnimationPresets(false) },
                onApplyPreset = { preset ->
                    viewModel.applyAnimationPreset(
                        layerId = state.selectedLayer!!.id,
                        preset = preset,
                        startTimeMs = state.currentTimeMs
                    )
                }
            )
        }

        // Phase 16: Time Remap Dialog
        if (state.showTimeRemapDialog && state.selectedLayer != null) {
            TimeRemapDialog(
                layer = state.selectedLayer!!,
                currentTimeMs = state.currentTimeMs,
                onDismiss = { viewModel.setShowTimeRemap(false) },
                onToggleTimeRemap = {
                    viewModel.toggleTimeRemap(state.selectedLayer!!.id)
                },
                onSetSpeedMultiplier = { spd ->
                    viewModel.setTimeRemapMultiplier(state.selectedLayer!!.id, spd)
                },
                onApplySpeedPreset = { preset ->
                    viewModel.applySpeedRampPreset(state.selectedLayer!!.id, preset)
                }
            )
        }
    }
}

@Composable
private fun TopEditorBar(
    projectName: String,
    resolutionText: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenProject: () -> Unit,
    onAddLayer: () -> Unit
) {
    Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(width = 0.5.dp, color = DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Branding & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.2f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("◆", color = ElectricCyan, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "LumaLayer Studio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$projectName • $resolutionText",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // Quick Actions: Undo, Redo, Export, Add Layer, Project JSON
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Undo
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("top_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("top_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Export Video (4K) Button
                IconButton(
                    onClick = onOpenExport,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("top_export_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export Video",
                        tint = ElectricCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Add Layer
                IconButton(
                    onClick = onAddLayer,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("top_add_layer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Layer",
                        tint = ElectricCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Project JSON
                IconButton(
                    onClick = onOpenProject,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("top_project_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Project JSON",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTabBar(
    currentTab: DockTab,
    layersCount: Int,
    selectedLayerName: String?,
    onSelectTab: (DockTab) -> Unit
) {
    TabRow(
        selectedTabIndex = if (currentTab == DockTab.LAYERS) 0 else 1,
        containerColor = DarkSurfaceElevated,
        contentColor = ElectricCyan,
        indicator = { tabPositions ->
            val index = if (currentTab == DockTab.LAYERS) 0 else 1
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                color = ElectricCyan
            )
        },
        modifier = Modifier.height(38.dp)
    ) {
        Tab(
            selected = currentTab == DockTab.LAYERS,
            onClick = { onSelectTab(DockTab.LAYERS) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Layers ($layersCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            modifier = Modifier.testTag("tab_layers")
        )
        Tab(
            selected = currentTab == DockTab.INSPECTOR,
            onClick = { onSelectTab(DockTab.INSPECTOR) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (selectedLayerName != null) "Inspector: $selectedLayerName" else "Inspector",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            },
            modifier = Modifier.testTag("tab_inspector")
        )
    }
}
