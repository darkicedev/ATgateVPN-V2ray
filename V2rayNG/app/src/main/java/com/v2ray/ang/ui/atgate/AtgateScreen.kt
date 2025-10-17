package com.v2ray.ang.ui.atgate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.rememberModalBottomSheetState
import com.v2ray.ang.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtgateScreen(
    uiState: AtgateUiState,
    snackbarHostState: SnackbarHostState,
    onToggleConnection: () -> Unit,
    onOpenImport: () -> Unit,
    onDismissImport: () -> Unit,
    onImport: (String) -> Unit,
    onSpeedTest: () -> Unit
) {
    val containerGradient = rememberBackgroundGradient()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var vlessValue by rememberSaveable(uiState.showImportSheet) { mutableStateOf("") }

    LaunchedEffect(uiState.showImportSheet) {
        if (!uiState.showImportSheet) {
            vlessValue = ""
            if (sheetState.isVisible) {
                sheetState.hide()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerGradient)
                .padding(padding)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TopHeader(
                    hasProfile = uiState.hasProfile,
                    onImportClick = onOpenImport
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusSummary(status = uiState.status, uiState = uiState)

                    Spacer(modifier = Modifier.height(36.dp))

                    ConnectionOrb(
                        uiState = uiState,
                        onToggle = onToggleConnection
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    MetricsSection(
                        uiState = uiState,
                        onSpeedTest = onSpeedTest,
                        onImport = onOpenImport
                    )
                }

                FooterHint()
            }
        }
    }

    ImportBottomSheet(
        uiState = uiState,
        sheetState = sheetState,
        vlessState = vlessValue to { vlessValue = it },
        onDismiss = onDismissImport,
        onImport = onImport
    )
}

@Composable
private fun rememberBackgroundGradient(): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val topGlow = primary.copy(alpha = 0.22f)
    val midGlow = surface.copy(alpha = 0.92f)
    val colors = listOf(topGlow, midGlow, background)
    val screenHeight = LocalConfiguration.current.screenHeightDp.coerceAtLeast(720)
    return remember(primary, surface, background, screenHeight) {
        Brush.verticalGradient(
            colors = colors,
            startY = 0f,
            endY = screenHeight * 2.5f
        )
    }
}

@Composable
private fun TopHeader(
    hasProfile: Boolean,
    onImportClick: () -> Unit
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val taglineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentBrush = remember(primaryColor) {
        Brush.horizontalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.65f),
                primaryColor.copy(alpha = 0.15f)
            )
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(id = R.string.atgate_connect_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            Text(
                text = stringResource(id = R.string.atgate_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = taglineColor
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(4.dp)
                    .width(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentBrush)
            )
        }
        IconButton(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    CircleShape
                ),
            onClick = onImportClick
        ) {
            val icon = if (hasProfile) Icons.Rounded.Edit else Icons.Rounded.CloudDownload
            val description = if (hasProfile) {
                stringResource(id = R.string.atgate_action_change_profile)
            } else {
                stringResource(id = R.string.atgate_action_import)
            }
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatusSummary(
    status: ConnectionStatus,
    uiState: AtgateUiState
) {
    val (statusLabel, statusSubtitle) = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(id = R.string.atgate_status_connected) to
            stringResource(id = R.string.atgate_status_detail_connected)

        ConnectionStatus.READY -> stringResource(id = R.string.atgate_status_ready) to
            stringResource(id = R.string.atgate_status_detail_ready)

        ConnectionStatus.IDLE -> stringResource(id = R.string.atgate_status_disconnected) to
            stringResource(id = R.string.atgate_status_detail_disconnected)
    }
    val shape = RoundedCornerShape(26.dp)
    val (startColor, endColor, borderColor) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        ConnectionStatus.READY -> Triple(
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
        )

        ConnectionStatus.IDLE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    }
    val statusBrush = remember(status) {
        Brush.linearGradient(listOf(startColor, endColor))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(statusBrush, shape = shape)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun ConnectionOrb(
    uiState: AtgateUiState,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isActive = uiState.isRunning
    val pulseTransition = rememberInfiniteTransition(label = "connectionOrbPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val baseColors = if (isActive) {
        listOf(
            primaryColor.copy(alpha = 0.95f),
            primaryColor,
            primaryColor.copy(alpha = 0.8f)
        )
    } else {
        listOf(
            primaryColor.copy(alpha = 0.7f),
            primaryColor.copy(alpha = 0.9f),
            primaryColor.copy(alpha = 0.7f)
        )
    }
    val glowStrokeColor = primaryColor.copy(alpha = glowAlpha)

    Box(
        modifier = Modifier
            .size(if (LocalConfiguration.current.screenWidthDp < 380) 220.dp else 260.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .clickable(
                enabled = !uiState.connectionInProgress,
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(baseColors),
                radius = size.minDimension / 2f
            )
            drawCircle(
                color = glowStrokeColor,
                radius = size.minDimension / 2f,
                style = Stroke(width = 6.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (uiState.isRunning) Icons.Rounded.PowerSettingsNew else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = onPrimaryColor.copy(alpha = 0.92f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (uiState.isRunning) {
                    stringResource(id = R.string.atgate_connect_button_disconnect)
                } else {
                    stringResource(id = R.string.atgate_connect_button_connect)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = onPrimaryColor
            )
        }

        if (uiState.connectionInProgress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.Center),
                strokeCap = StrokeCap.Round,
                strokeWidth = 4.dp,
                color = onPrimaryColor.copy(alpha = 0.85f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportBottomSheet(
    uiState: AtgateUiState,
    sheetState: androidx.compose.material3.SheetState,
    vlessState: Pair<String, (String) -> Unit>,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    if (!uiState.showImportSheet) return
    val (value, onValueChange) = vlessState
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        sheetState.show()
    }
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(id = R.string.atgate_import_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = stringResource(id = R.string.atgate_import_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                label = { Text(text = stringResource(id = R.string.atgate_import_placeholder)) },
                placeholder = { Text(text = "vless://example@server:443?type=ws") },
                enabled = !uiState.importInProgress,
                minLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.importInProgress,
                    onClick = { onImport(value) }
                ) {
                    if (uiState.importInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                    }
                    Text(text = stringResource(id = R.string.atgate_import_button))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricsSection(
    uiState: AtgateUiState,
    onSpeedTest: () -> Unit,
    onImport: () -> Unit
) {
    val durationText = rememberConnectionDuration(uiState.isRunning, uiState.connectedSince)
    val latencyValue = uiState.latencyMs?.let { "$it мс" }
        ?: uiState.lastPingLabel.takeIf { it.isNotBlank() }
        ?: stringResource(id = R.string.atgate_placeholder_latency)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                modifier = Modifier.fillMaxWidth(0.48f),
                title = stringResource(id = R.string.atgate_label_profile),
                value = uiState.profileName.ifBlank { stringResource(id = R.string.atgate_profile_default_name) },
                icon = Icons.Rounded.VpnKey,
                highlight = uiState.hasProfile
            )
            MetricCard(
                modifier = Modifier.fillMaxWidth(0.48f),
                title = stringResource(id = R.string.atgate_label_endpoint),
                value = uiState.profileEndpoint.ifBlank { stringResource(id = R.string.atgate_placeholder_endpoint) },
                icon = Icons.Rounded.Language
            )
            MetricCard(
                modifier = Modifier.fillMaxWidth(0.48f),
                title = stringResource(id = R.string.atgate_label_latency),
                value = latencyValue,
                icon = Icons.Rounded.Speed
            )
            MetricCard(
                modifier = Modifier.fillMaxWidth(0.48f),
                title = stringResource(id = R.string.atgate_label_duration),
                value = durationText,
                icon = Icons.Rounded.Schedule
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = onSpeedTest,
                enabled = uiState.hasProfile,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.NetworkCheck,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.atgate_action_speedtest))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onImport,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(id = R.string.atgate_action_import))
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val backgroundBrush = remember(highlight, primary, surfaceVariant) {
        if (highlight) {
            Brush.linearGradient(
                listOf(
                    primary.copy(alpha = 0.35f),
                    primary.copy(alpha = 0.08f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    surfaceVariant.copy(alpha = 0.32f),
                    surfaceVariant.copy(alpha = 0.12f)
                )
            )
        }
    }
    val borderColor = remember(highlight, primary, outline) {
        if (highlight) {
            primary.copy(alpha = 0.45f)
        } else {
            outline.copy(alpha = 0.25f)
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush, shape)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun rememberConnectionDuration(
    isRunning: Boolean,
    connectedSince: Long?
): String {
    var duration by remember(connectedSince) { mutableStateOf("—") }
    LaunchedEffect(isRunning, connectedSince) {
        if (!isRunning || connectedSince == null) {
            duration = "—"
        } else {
            while (true) {
                val elapsed = max(0L, System.currentTimeMillis() - connectedSince)
                duration = formatElapsed(elapsed)
                delay(1000L)
            }
        }
    }
    return duration
}

private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun FooterHint() {
    // Footer intentionally left empty for clean layout.
}
