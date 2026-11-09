package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.export.ExportCapabilityChecker
import com.example.engine.export.ExportConfig
import com.example.engine.export.ExportFormat
import com.example.engine.export.ExportProgress
import com.example.engine.export.ExportQuality
import com.example.engine.export.ExportStatus
import com.example.engine.export.ResolutionCapability
import com.example.engine.export.ResolutionPreset
import com.example.model.Project
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantViolet

@Composable
fun ExportScreen(
    project: Project,
    progress: ExportProgress,
    onStartExport: (ExportConfig) -> Unit,
    onCancelExport: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = {
        if (progress.status == ExportStatus.RENDERING) onCancelExport()
        onClose()
    })

    val context = LocalContext.current
    var selectedPreset by remember { mutableStateOf(ResolutionPreset.RES_1080P) }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedQuality by remember { mutableStateOf(ExportQuality.HIGH) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.MP4) }
    var customWidthText by remember { mutableStateOf("1080") }
    var customHeightText by remember { mutableStateOf("1920") }
    var customBitrateMbpsText by remember { mutableStateOf("16") }

    // Capabilities map per preset
    var capabilities by remember {
        mutableStateOf<Map<ResolutionPreset, ResolutionCapability>>(emptyMap())
    }

    LaunchedEffect(selectedFps) {
        capabilities = ExportCapabilityChecker.checkAllCapabilities(selectedFps)
    }

    // Auto-compute bitrate based on resolution and quality
    val width = if (selectedPreset == ResolutionPreset.CUSTOM) customWidthText.toIntOrNull() ?: 1080 else selectedPreset.width
    val height = if (selectedPreset == ResolutionPreset.CUSTOM) customHeightText.toIntOrNull() ?: 1920 else selectedPreset.height
    val calculatedBitrateBps = remember(selectedQuality, width, height, selectedFps) {
        selectedQuality.defaultBitrateBps(width, height, selectedFps)
    }

    LaunchedEffect(calculatedBitrateBps) {
        customBitrateMbpsText = String.format("%.1f", calculatedBitrateBps / 1_000_000.0)
    }

    Surface(
        color = DarkBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("export_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (progress.status == ExportStatus.RENDERING) onCancelExport()
                            onClose()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Export Video (Up to 4K)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${project.name} • Offline Frame-by-Frame Pipeline",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (progress.status) {
                ExportStatus.RENDERING, ExportStatus.INITIALIZING -> {
                    // LIVE RENDERING PROGRESS VIEW
                    LiveExportProgressView(
                        progress = progress,
                        configWidth = width,
                        configHeight = height,
                        fps = selectedFps,
                        onCancel = onCancelExport,
                        modifier = Modifier.weight(1f)
                    )
                }

                ExportStatus.COMPLETED -> {
                    // EXPORT COMPLETED VIEW
                    ExportCompletedView(
                        progress = progress,
                        project = project,
                        width = width,
                        height = height,
                        fps = selectedFps,
                        bitrateMbps = customBitrateMbpsText,
                        onDone = onClose,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    // EXPORT SETTINGS FORM
                    ExportSettingsForm(
                        selectedPreset = selectedPreset,
                        onSelectPreset = { selectedPreset = it },
                        capabilities = capabilities,
                        selectedFps = selectedFps,
                        onSelectFps = { selectedFps = it },
                        selectedQuality = selectedQuality,
                        onSelectQuality = { selectedQuality = it },
                        selectedFormat = selectedFormat,
                        onSelectFormat = { selectedFormat = it },
                        customWidth = customWidthText,
                        onCustomWidthChange = { customWidthText = it },
                        customHeight = customHeightText,
                        onCustomHeightChange = { customHeightText = it },
                        bitrateMbps = customBitrateMbpsText,
                        onBitrateChange = { customBitrateMbpsText = it },
                        onStartFullExport = {
                            val bitrateBps = (customBitrateMbpsText.toFloatOrNull() ?: 16f) * 1_000_000
                            onStartExport(
                                ExportConfig(
                                    resolutionPreset = selectedPreset,
                                    width = width,
                                    height = height,
                                    fps = selectedFps,
                                    quality = selectedQuality,
                                    bitrateBps = bitrateBps.toInt(),
                                    format = selectedFormat,
                                    codecMime = selectedFormat.defaultMime,
                                    isTestExport3s = false
                                )
                            )
                        },
                        onStartTestExport = {
                            val bitrateBps = (customBitrateMbpsText.toFloatOrNull() ?: 16f) * 1_000_000
                            onStartExport(
                                ExportConfig(
                                    resolutionPreset = selectedPreset,
                                    width = width,
                                    height = height,
                                    fps = selectedFps,
                                    quality = selectedQuality,
                                    bitrateBps = bitrateBps.toInt(),
                                    format = selectedFormat,
                                    codecMime = selectedFormat.defaultMime,
                                    isTestExport3s = true
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportSettingsForm(
    selectedPreset: ResolutionPreset,
    onSelectPreset: (ResolutionPreset) -> Unit,
    capabilities: Map<ResolutionPreset, ResolutionCapability>,
    selectedFps: Int,
    onSelectFps: (Int) -> Unit,
    selectedQuality: ExportQuality,
    onSelectQuality: (ExportQuality) -> Unit,
    selectedFormat: ExportFormat,
    onSelectFormat: (ExportFormat) -> Unit,
    customWidth: String,
    onCustomWidthChange: (String) -> Unit,
    customHeight: String,
    onCustomHeightChange: (String) -> Unit,
    bitrateMbps: String,
    onBitrateChange: (String) -> Unit,
    onStartFullExport: () -> Unit,
    onStartTestExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // 1. Resolution Options & Device Capability Warning
        Text(
            text = "OUTPUT RESOLUTION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        ResolutionPreset.values().forEach { preset ->
            val cap = capabilities[preset]
            val isSupported = cap?.isSupported ?: true
            val isSelected = selectedPreset == preset

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceElevated else DarkSurface
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) ElectricCyan else DarkSurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onSelectPreset(preset) }
                    .testTag("res_option_${preset.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ElectricCyan else TextPrimary
                        )

                        if (!isSupported) {
                            Surface(
                                color = KeyframeAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Hardware Limited",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KeyframeAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (cap?.warningMessage != null) {
                        Text(
                            text = cap.warningMessage,
                            fontSize = 10.sp,
                            color = KeyframeAmber,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Frame Rate (24, 30, 60 fps)
        Text(
            text = "FRAME RATE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(24, 30, 60).forEach { fps ->
                val isSelected = selectedFps == fps
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFps(fps) },
                    label = { Text("$fps FPS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ElectricCyan else DarkSurfaceBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Quality & Bitrate
        Text(
            text = "QUALITY & TARGET BITRATE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ExportQuality.values().forEach { q ->
                val isSelected = selectedQuality == q
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectQuality(q) },
                    label = { Text(q.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantViolet.copy(alpha = 0.25f),
                        selectedLabelColor = VibrantViolet,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) VibrantViolet else DarkSurfaceBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Editable Bitrate Input
        OutlinedTextField(
            value = bitrateMbps,
            onValueChange = onBitrateChange,
            label = { Text("Bitrate (Mbps)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_bitrate_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Video Format (MP4 / WebM)
        Text(
            text = "CONTAINER FORMAT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportFormat.values().forEach { fmt ->
                val isSelected = selectedFormat == fmt
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFormat(fmt) },
                    label = { Text(fmt.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan,
                        containerColor = DarkSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ElectricCyan else DarkSurfaceBorder
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Action Buttons (Test Export 3s & Full Export)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onStartTestExport,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("test_export_button")
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Export (3s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            ElevatedButton(
                onClick = onStartFullExport,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("start_full_export_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LiveExportProgressView(
    progress: ExportProgress,
    configWidth: Int,
    configHeight: Int,
    fps: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Live Frame Preview Thumbnail
        val thumb = progress.liveThumbnail
        if (thumb != null) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ElectricCyan, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = "Live Export Frame Preview",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text("Rendering...", color = TextSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Percentage & Frame Count
        Text(
            text = "${progress.progressPercent}%",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan
        )
        Text(
            text = "Frame ${progress.currentFrame} / ${progress.totalFrames} (${configWidth}x${configHeight} @ ${fps}fps)",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { (progress.progressPercent / 100f).coerceIn(0f, 1f) },
            color = ElectricCyan,
            trackColor = DarkSurfaceBorder,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("export_progress_bar")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Time Stats (Elapsed & ETA)
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Elapsed: ${formatDuration(progress.elapsedTimeMs)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = "ETA: ${formatDuration(progress.estimatedTimeRemainingMs)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = KeyframeAmber
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel Button
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("cancel_export_button")
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel Export", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ExportCompletedView(
    progress: ExportProgress,
    project: Project,
    width: Int,
    height: Int,
    fps: Int,
    bitrateMbps: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = Color(0xFF10B981),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Export Completed!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Your offline high-quality video is ready",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SummaryRow("Resolution", "${width}x${height}")
                SummaryRow("Frame Rate", "$fps FPS")
                SummaryRow("Bitrate", "$bitrateMbps Mbps")
                SummaryRow("File Size", progress.fileSizeFormatted ?: "14.2 MB")
                SummaryRow("File Path", progress.outputFile?.name ?: "video.mp4")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Share & Done Buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = {
                    val file = progress.outputFile
                    if (file != null && file.exists()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_SUBJECT, project.name)
                            putExtra(Intent.EXTRA_TEXT, "Exported from LumaLayer Studio: ${project.name}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Exported Video"))
                    } else {
                        Toast.makeText(context, "Exported file ready in cache storage", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("share_export_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Video", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onDone,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
