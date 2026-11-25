package com.example.ui.kids

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppMode
import com.example.data.model.ManagedDevice
import com.example.data.model.ParentCommand
import com.example.platform.DeviceTelemetryHelper
import com.example.platform.PermissionKind
import com.example.ui.theme.KidsMintAccent
import com.example.ui.theme.KidsSkyAccent
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ParentBackground
import com.example.ui.theme.ParentInkPrimary
import com.example.ui.theme.ParentInkSecondary
import com.example.ui.theme.ParentOutline
import com.example.ui.theme.ParentSurface
import com.example.ui.theme.ParentSurfaceVariant
import com.example.ui.theme.ParentVioletDeep
import com.example.ui.theme.ParentVioletPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.FamilyGuardViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun KidsCompanionScreen(
    viewModel: FamilyGuardViewModel,
    userDisplayName: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val commandsState by viewModel.commandsState.collectAsStateWithLifecycle()
    val rulesState by viewModel.rulesState.collectAsStateWithLifecycle()
    val permissions by viewModel.permissionStatuses.collectAsStateWithLifecycle()
    val usageList by viewModel.usageStats.collectAsStateWithLifecycle()

    var pairingCodeInput by remember { mutableStateOf("") }
    var kidNameInput by remember { mutableStateOf(userDisplayName.ifBlank { "My Phone" }) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshLocalDeviceDiagnostics(context)
        selectedDevice?.let { viewModel.syncThisDeviceNow(context, it.id) }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalDeviceDiagnostics(context)
    }

    val commands = (commandsState as? UiState.Success)?.data.orEmpty()
    val pendingCommands = commands.filter { it.status == "pending" }
    val parentMessages = commands.filter { it.commandType == "SHOW_MESSAGE" && it.status == "executed" }
    val rules = (rulesState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        containerColor = ParentBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(KidsMintAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ChildCare, contentDescription = null, tint = ParentVioletDeep)
                            }
                            Column {
                                Text(
                                    text = selectedDevice?.kidName ?: "Kids Companion",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ParentInkPrimary
                                )
                                Text(
                                    text = selectedDevice?.let { "${it.deviceName} • Connected to Parent" } ?: "Pair with Parent Code",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ParentInkSecondary
                                )
                            }
                        }

                        Row {
                            OutlinedButton(
                                onClick = { viewModel.setAppMode(AppMode.PARENT) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("switch_to_parent_mode_btn")
                            ) {
                                Icon(Icons.Rounded.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Parent")
                            }
                            IconButton(onClick = onSignOut) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out", tint = ParentInkSecondary)
                            }
                        }
                    }
                }

                // Active Transparency Banner (shows active Location / Camera / Audio / Screen Share with instant Stop)
                selectedDevice?.let { dev ->
                    if (dev.activeLocationSession || dev.activeCameraSession || dev.activeAudioSession || dev.activeScreenShareSession) {
                        item {
                            ActiveTransparencyBanner(
                                device = dev,
                                onStopSession = { kind -> viewModel.stopActiveSession(context, dev, kind) }
                            )
                        }
                    }
                }

                // Pair with Parent Code if not linked or wanting to join another parent
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ParentSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ParentOutline, RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Pair With Parent Code", style = MaterialTheme.typography.titleMedium, color = ParentInkPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = pairingCodeInput,
                                    onValueChange = { pairingCodeInput = it.uppercase().take(6) },
                                    label = { Text("6-Char Code") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("kids_pairing_code_input")
                                )
                                OutlinedTextField(
                                    value = kidNameInput,
                                    onValueChange = { kidNameInput = it },
                                    label = { Text("Your Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.claimPairingCode(context, pairingCodeInput, kidNameInput) },
                                    enabled = pairingCodeInput.length == 6,
                                    colors = ButtonDefaults.buttonColors(containerColor = ParentVioletPrimary),
                                    modifier = Modifier.weight(1f).testTag("claim_pairing_code_btn")
                                ) { Text("Join Family") }
                                selectedDevice?.let { dev ->
                                    OutlinedButton(
                                        onClick = { viewModel.syncThisDeviceNow(context, dev.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Sync Now")
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending Parent Consent Requests
                if (pendingCommands.isNotEmpty() && selectedDevice != null) {
                    item {
                        Text("Parent Requests Requiring Your Approval", style = MaterialTheme.typography.titleLarge, color = ParentInkPrimary)
                    }
                    items(pendingCommands, key = { it.id }) { cmd ->
                        PendingConsentRequestCard(
                            command = cmd,
                            onApprove = { viewModel.executeOrRespondCommandOnChild(context, selectedDevice!!, cmd, true) },
                            onDecline = { viewModel.executeOrRespondCommandOnChild(context, selectedDevice!!, cmd, false) }
                        )
                    }
                }

                // Child Device Status + Daily Screen Time Summary + Approved Wallpaper
                selectedDevice?.let { dev ->
                    item {
                        KidsSummaryCard(
                            device = dev,
                            onShareLocationNow = { viewModel.startLocationShareFromChild(context, dev) },
                            onToggleFloatingPanel = { enabled -> viewModel.toggleFloatingPanel(context, dev, enabled) }
                        )
                    }
                }

                // Parent Messages
                if (parentMessages.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = ParentSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Messages from Parent", style = MaterialTheme.typography.titleMedium, color = ParentVioletDeep)
                                parentMessages.take(3).forEach { msg ->
                                    Text("• ${msg.payloadText}", style = MaterialTheme.typography.bodyLarge, color = ParentInkPrimary)
                                }
                            }
                        }
                    }
                }

                // App Usage & Schedules visible to Child
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ParentSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ParentOutline, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Your Screen-Time Rules & Top Apps", style = MaterialTheme.typography.titleMedium, color = ParentInkPrimary)
                            if (rules.isEmpty() && usageList.isEmpty()) {
                                Text(
                                    text = "No screen-time restrictions active right now. Grant Usage Access below to see your live app timer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ParentInkSecondary
                                )
                            }
                            rules.forEach { r ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(r.appName, style = MaterialTheme.typography.titleMedium)
                                    Text("${r.dailyLimitMinutes}m • ${if (r.paused) "Paused" else r.scheduleLabel}", color = ParentInkSecondary)
                                }
                            }
                            usageList.take(4).forEach { u ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(u.appName, style = MaterialTheme.typography.bodyMedium)
                                    Text("${u.totalMinutesToday} min today", style = MaterialTheme.typography.labelMedium, color = ParentVioletPrimary)
                                }
                            }
                        }
                    }
                }

                // Transparent Permission Checklist
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ParentSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ParentOutline, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("What Is Shared & Permissions", style = MaterialTheme.typography.titleMedium, color = ParentInkPrimary)
                                TextButton(onClick = {
                                    val runtime = buildList {
                                        add(Manifest.permission.CAMERA)
                                        add(Manifest.permission.RECORD_AUDIO)
                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            add(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }.toTypedArray()
                                    multiPermissionLauncher.launch(runtime)
                                }) { Text("Grant Runtime") }
                            }

                            permissions.forEach { perm ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(perm.title, style = MaterialTheme.typography.titleMedium, color = ParentInkPrimary)
                                        Text(perm.description, style = MaterialTheme.typography.bodyMedium, color = ParentInkSecondary)
                                    }
                                    if (perm.granted) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = StatusOnline)
                                    } else {
                                        TextButton(onClick = {
                                            when (perm.kind) {
                                                PermissionKind.SPECIAL_USAGE_ACCESS -> DeviceTelemetryHelper.openUsageAccessSettings(context)
                                                PermissionKind.SPECIAL_WRITE_SETTINGS -> DeviceTelemetryHelper.openWriteSettings(context)
                                                PermissionKind.SPECIAL_NOTIFICATION_LISTENER -> DeviceTelemetryHelper.openNotificationListenerSettings(context)
                                                else -> context.startActivity(
                                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        }) { Text("Allow") }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(90.dp)) }
            }

            // VISIBLE FLOATING USAGE PANEL (Draggable, Transparent, Easy Off Control)
            if (selectedDevice?.floatingPanelEnabled != false) {
                VisibleFloatingUsagePanel(
                    activeApp = selectedDevice?.activeAppName?.ifBlank { "FamilyGuard" } ?: "FamilyGuard",
                    screenTimeMinutes = selectedDevice?.dailyScreenTimeMinutes?.toInt() ?: 0,
                    parentConnected = selectedDevice != null,
                    onTurnOff = {
                        selectedDevice?.let { viewModel.toggleFloatingPanel(context, it, false) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveTransparencyBanner(
    device: ManagedDevice,
    onStopSession: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = StatusError.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Visibility, contentDescription = null, tint = StatusError)
                Text("Active Monitoring Sessions", style = MaterialTheme.typography.titleMedium, color = StatusError)
            }
            if (device.activeLocationSession) SessionStopRow("Location Sharing Active", Icons.Rounded.LocationOn) { onStopSession("location") }
            if (device.activeCameraSession) SessionStopRow("Camera Snapshot Session Active", Icons.Rounded.CameraAlt) { onStopSession("camera") }
            if (device.activeAudioSession) SessionStopRow("Live Audio Session Active", Icons.Rounded.Mic) { onStopSession("audio") }
            if (device.activeScreenShareSession) SessionStopRow("Screen Sharing Session Active", Icons.Rounded.ScreenShare) { onStopSession("screen") }
        }
    }
}

@Composable
private fun SessionStopRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onStop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = ParentInkPrimary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = ParentInkPrimary)
        }
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Stop")
        }
    }
}

@Composable
private fun PendingConsentRequestCard(
    command: ParentCommand,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ParentSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ParentVioletPrimary, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Parent Request: ${command.commandType}", style = MaterialTheme.typography.titleMedium, color = ParentVioletDeep)
            if (command.payloadText.isNotBlank()) {
                Text(command.payloadText, style = MaterialTheme.typography.bodyMedium, color = ParentInkSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline") }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ParentVioletPrimary),
                    modifier = Modifier.weight(1f).testTag("approve_command_${command.id}")
                ) { Text("Approve") }
            }
        }
    }
}

@Composable
private fun KidsSummaryCard(
    device: ManagedDevice,
    onShareLocationNow: () -> Unit,
    onToggleFloatingPanel: (Boolean) -> Unit
) {
    val wallpaperRes = if (device.wallpaperId == "violet_aurora") {
        R.drawable.img_wallpaper_violet_aurora
    } else {
        R.drawable.img_wallpaper_starstreak
    }
    val progress = (device.dailyScreenTimeMinutes.toFloat() / device.dailyLimitMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ParentSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ParentOutline, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (device.batteryCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                        contentDescription = null,
                        tint = ParentVioletPrimary
                    )
                    Text("${device.batteryPercent}% • ${device.networkType}", style = MaterialTheme.typography.titleMedium)
                }
                Image(
                    painter = painterResource(id = wallpaperRes),
                    contentDescription = "Approved wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily Screen Time", style = MaterialTheme.typography.titleMedium)
                    Text("${device.dailyScreenTimeMinutes} / ${device.dailyLimitMinutes} min", color = ParentVioletPrimary)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = ParentVioletPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Visible Floating Usage Panel", style = MaterialTheme.typography.titleMedium)
                    Text("Shows current app & daily timer (never captures keys/passwords)", style = MaterialTheme.typography.bodyMedium, color = ParentInkSecondary)
                }
                Switch(
                    checked = device.floatingPanelEnabled,
                    onCheckedChange = onToggleFloatingPanel
                )
            }

            OutlinedButton(
                onClick = onShareLocationNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (device.activeLocationSession) "Update Live Location" else "Share My Location With Parent")
            }
        }
    }
}

@Composable
fun VisibleFloatingUsagePanel(
    activeApp: String,
    screenTimeMinutes: Int,
    parentConnected: Boolean,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_usage_panel")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF6B46FF), Color(0xFF8C62FF))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.DragIndicator, contentDescription = "Drag floating panel", tint = Color.White.copy(alpha = 0.8f))
                    Column {
                        Text(
                            text = "$activeApp • ${screenTimeMinutes}m today",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (parentConnected) "Parent Connected • Transparent Panel" else "Standalone Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.86f)
                        )
                    }
                }
                TextButton(
                    onClick = onTurnOff,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("turn_off_floating_panel_btn")
                ) {
                    Text("Turn Off", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
