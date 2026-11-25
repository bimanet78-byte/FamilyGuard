package com.example.ui.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppMode
import com.example.data.model.FeatureCatalog
import com.example.data.model.ManagedDevice
import com.example.data.model.PairingCode
import com.example.platform.DeviceTelemetryHelper
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ParentBackground
import com.example.ui.theme.ParentInkPrimary
import com.example.ui.theme.ParentInkSecondary
import com.example.ui.theme.ParentOutline
import com.example.ui.theme.ParentSurface
import com.example.ui.theme.ParentSurfaceVariant
import com.example.ui.theme.ParentVioletDeep
import com.example.ui.theme.ParentVioletPrimary
import com.example.ui.theme.ParentVioletSecondary
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.FamilyGuardViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: FamilyGuardViewModel,
    userDisplayName: String,
    userEmail: String,
    onOpenDeviceControl: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val devicesState by viewModel.devicesState.collectAsStateWithLifecycle()
    val pairingCodesState by viewModel.pairingCodesState.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val commandsState by viewModel.commandsState.collectAsStateWithLifecycle()

    var showPairingDialog by remember { mutableStateOf(false) }
    var showLinkThisDeviceDialog by remember { mutableStateOf(false) }
    var showFeatureMatrixDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val localTelemetry = remember { DeviceTelemetryHelper.collectTelemetry(context) }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalDeviceDiagnostics(context)
    }

    val devices = (devicesState as? UiState.Success)?.data.orEmpty()
    val filteredDevices = remember(devices, searchQuery) {
        if (searchQuery.isBlank()) devices
        else devices.filter {
            it.kidName.contains(searchQuery, ignoreCase = true) ||
                it.deviceName.contains(searchQuery, ignoreCase = true) ||
                it.activeAppName.contains(searchQuery, ignoreCase = true)
        }
    }
    val matchingCapabilities = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else FeatureCatalog.capabilities.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.officialApi.contains(searchQuery, ignoreCase = true)
        }
    }

    val pendingCount = (commandsState as? UiState.Success)?.data?.count { it.status == "pending" } ?: 0

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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Parent Profile + Mode Switcher Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ParentVioletPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userDisplayName.firstOrNull()?.uppercase() ?: "P",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column {
                                Text(
                                    text = userDisplayName.ifBlank { "Parent Account" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ParentInkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = userEmail.ifBlank { "Parent Controller" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ParentInkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier.testTag("parent_notifications_btn")
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Rounded.Notifications,
                                        contentDescription = "Notifications",
                                        tint = ParentInkPrimary
                                    )
                                    if (pendingCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(ParentVioletPrimary)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { showFeatureMatrixDialog = true },
                                modifier = Modifier.testTag("permissions_matrix_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = "Permissions and API Matrix",
                                    tint = ParentInkPrimary
                                )
                            }
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("sign_out_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Sign out",
                                    tint = ParentInkSecondary
                                )
                            }
                        }
                    }
                }

                // Mode Switcher Pill (Parent Controller <-> Kids Companion)
                item {
                    ModeSwitcherBar(
                        currentMode = AppMode.PARENT,
                        onSwitchToKid = { viewModel.setAppMode(AppMode.KID) }
                    )
                }

                // SCREENSHOT 1 REFERENCE: Purple-Violet Search & Active App Card on White Surface
                item {
                    PurpleSearchHeroCard(
                        query = searchQuery,
                        onQueryChange = viewModel::updateSearchQuery,
                        batteryPercent = selectedDevice?.batteryPercent?.toInt() ?: localTelemetry.batteryPercent,
                        runningAppName = selectedDevice?.activeAppName?.ifBlank { "Quickstep" }
                            ?: localTelemetry.activeAppName
                    )
                }

                // Live Search Results for Features if user searches
                if (matchingCapabilities.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = ParentSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Matching Controls (${matchingCapabilities.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ParentInkPrimary
                                )
                                matchingCapabilities.take(5).forEach { cap ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedDevice?.let { onOpenDeviceControl(it.id) }
                                                    ?: run { showLinkThisDeviceDialog = true }
                                            }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cap.title, style = MaterialTheme.typography.titleMedium, color = ParentInkPrimary)
                                            Text(cap.officialApi, style = MaterialTheme.typography.bodyMedium, color = ParentInkSecondary)
                                        }
                                        StatusBadgeChip(cap.status.label)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Action Row: Pair Device + Link Current Device
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showPairingDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ParentVioletPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pair_child_device_btn")
                        ) {
                            Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pair Child", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { showLinkThisDeviceDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ParentSurfaceVariant,
                                contentColor = ParentVioletDeep
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("link_this_device_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Link This Phone", maxLines = 1)
                        }
                    }
                }

                // Connected Child Devices Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Child Devices",
                            style = MaterialTheme.typography.titleLarge,
                            color = ParentInkPrimary
                        )
                        Text(
                            text = "${devices.size} linked",
                            style = MaterialTheme.typography.labelMedium,
                            color = ParentVioletPrimary
                        )
                    }
                }

                when (val state = devicesState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ParentVioletPrimary)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ParentSurface),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (filteredDevices.isEmpty()) {
                            item {
                                EmptyDevicesCard(
                                    onPairClick = { showPairingDialog = true },
                                    onLinkLocalClick = { showLinkThisDeviceDialog = true }
                                )
                            }
                        } else {
                            items(filteredDevices, key = { it.id }) { device ->
                                ConnectedDeviceCard(
                                    device = device,
                                    isSelected = device.id == selectedDevice?.id,
                                    onOpen = {
                                        viewModel.selectDevice(device.id)
                                        onOpenDeviceControl(device.id)
                                    }
                                )
                            }
                        }
                    }
                }

                // Transparent Family Safety Card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ParentSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ParentOutline, RoundedCornerShape(22.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFeatureMatrixDialog = true }
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ParentSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = ParentVioletPrimary)
                                }
                                Column {
                                    Text(
                                        text = "Permissions & Android API Matrix",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ParentInkPrimary
                                    )
                                    Text(
                                        text = "View consent rules, Play policy limits, and feature statuses",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ParentInkSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Open Matrix",
                                tint = ParentInkSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPairingDialog) {
        val codes = (pairingCodesState as? UiState.Success)?.data.orEmpty()
        PairingCodeDialog(
            parentName = userDisplayName,
            codes = codes,
            onGenerateCode = { hint -> viewModel.createPairingCode(userDisplayName, hint) },
            onRevokeCode = { code -> viewModel.revokePairingCode(code) },
            onDismiss = { showPairingDialog = false }
        )
    }

    if (showLinkThisDeviceDialog) {
        LinkThisDeviceDialog(
            detectedDeviceName = localTelemetry.deviceName,
            onConfirm = { kidName ->
                viewModel.linkCurrentDeviceWithTelemetryShortcut(context, kidName)
                showLinkThisDeviceDialog = false
            },
            onDismiss = { showLinkThisDeviceDialog = false }
        )
    }

    if (showFeatureMatrixDialog) {
        FeatureMatrixDialog(onDismiss = { showFeatureMatrixDialog = false })
    }

    if (showNotificationsDialog) {
        val commands = (commandsState as? UiState.Success)?.data.orEmpty()
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Recent Sync & Command Activity") },
            text = {
                if (commands.isEmpty()) {
                    Text("No parent commands sent yet. Open a device to send a request or view audit logs.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        commands.take(6).forEach { cmd ->
                            Column {
                                Text(
                                    text = "${cmd.commandType} • ${cmd.status}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = cmd.resultMessage.ifBlank { cmd.payloadText },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ParentInkSecondary
                                )
                                HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Close") }
            }
        )
    }
}

private fun FamilyGuardViewModel.linkCurrentDeviceWithTelemetryShortcut(
    context: android.content.Context,
    kidName: String
) {
    linkCurrentDeviceForFamily(context, kidName)
}

@Composable
fun ModeSwitcherBar(
    currentMode: AppMode,
    onSwitchToKid: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ParentSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ParentOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (currentMode == AppMode.PARENT) ParentVioletPrimary else Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Devices,
                        contentDescription = null,
                        tint = if (currentMode == AppMode.PARENT) Color.White else ParentInkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Parent Controller",
                        color = if (currentMode == AppMode.PARENT) Color.White else ParentInkSecondary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (currentMode == AppMode.KID) ParentVioletPrimary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSwitchToKid)
                    .testTag("switch_to_kids_mode_btn")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChildCare,
                        contentDescription = null,
                        tint = if (currentMode == AppMode.KID) Color.White else ParentInkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Kids Companion",
                        color = if (currentMode == AppMode.KID) Color.White else ParentInkSecondary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

/**
 * Matches Screenshot 1: Rounded purple/violet gradient card on clean white background
 * with "Search for apps or content", battery %, search field, thin divider, and active app status row.
 */
@Composable
fun PurpleSearchHeroCard(
    query: String,
    onQueryChange: (String) -> Unit,
    batteryPercent: Int,
    runningAppName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7B4BFF),
                        Color(0xFF9C72FF),
                        Color(0xFF7D4CFF)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search for apps or content",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatteryFull,
                        contentDescription = "Battery",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$batteryPercent%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Type app, kid name, or feature...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_search_input")
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.45f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF101114)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "$runningAppName is running",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ConnectedDeviceCard(
    device: ManagedDevice,
    isSelected: Boolean,
    onOpen: () -> Unit
) {
    val statusColor = when (device.connectionState) {
        "online" -> StatusOnline
        "connecting" -> StatusConnecting
        else -> StatusOffline
    }
    val syncLabel = remember(device.updatedAt) {
        val ts = device.updatedAt?.toDate() ?: Date()
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(ts)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ParentSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ParentVioletPrimary else ParentOutline,
                shape = RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen)
            .testTag("device_card_${device.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ParentSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = ParentVioletPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = device.kidName,
                            style = MaterialTheme.typography.titleLarge,
                            color = ParentInkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${device.deviceName} • Android ${device.androidVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParentInkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = device.connectionState.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = ParentInkPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (device.batteryCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                        contentDescription = null,
                        tint = ParentVioletPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("${device.batteryPercent}%", style = MaterialTheme.typography.labelMedium, color = ParentInkPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = ParentInkSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(device.networkType.ifBlank { "-" }, style = MaterialTheme.typography.labelMedium, color = ParentInkSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = ParentInkSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(syncLabel, style = MaterialTheme.typography.labelMedium, color = ParentInkSecondary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Device Control & Swipe Deck",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ParentVioletPrimary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Open device",
                    tint = ParentVioletPrimary
                )
            }
        }
    }
}

@Composable
private fun EmptyDevicesCard(
    onPairClick: () -> Unit,
    onLinkLocalClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ParentSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ParentOutline, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Devices,
                contentDescription = null,
                tint = ParentVioletPrimary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "No Child Devices Linked Yet",
                style = MaterialTheme.typography.titleLarge,
                color = ParentInkPrimary
            )
            Text(
                text = "Generate a 6-character pairing code for your child's phone, or link this device right now to explore the full Parent & Kids dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = ParentInkSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPairClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pairing Code")
                }
                Button(
                    onClick = onLinkLocalClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ParentVioletPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Link This Phone")
                }
            }
        }
    }
}

@Composable
private fun LinkThisDeviceDialog(
    detectedDeviceName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var kidName by remember { mutableStateOf("Child Phone") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link This Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Register $detectedDeviceName with real hardware telemetry so you can test both Parent Controller and Kids Companion flows.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = kidName,
                    onValueChange = { kidName = it },
                    label = { Text("Child Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(kidName) },
                colors = ButtonDefaults.buttonColors(containerColor = ParentVioletPrimary)
            ) {
                Text("Link Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PairingCodeDialog(
    parentName: String,
    codes: List<PairingCode>,
    onGenerateCode: (String) -> Unit,
    onRevokeCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hint by remember { mutableStateOf("") }
    val activeCode = codes.firstOrNull { it.status == "pending" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secure Device Pairing") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeCode != null) {
                    Text(
                        text = "Scan QR or enter this 15-minute code in the Kids Companion app:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DeterministicQrMatrix(
                        payload = "familyguard://pair?code=${activeCode.id}&parent=$parentName",
                        modifier = Modifier.size(140.dp)
                    )
                    Surface(
                        color = ParentSurfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = activeCode.id,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                            color = ParentVioletDeep,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                    TextButton(onClick = { onRevokeCode(activeCode.id) }) {
                        Text("Revoke Code")
                    }
                } else {
                    Text(
                        text = "Create a short-lived one-time pairing code (valid 15 minutes). No permanent secrets are stored on the client.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = hint,
                        onValueChange = { hint = it },
                        label = { Text("Child Device Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onGenerateCode(hint) },
                        colors = ButtonDefaults.buttonColors(containerColor = ParentVioletPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate One-Time Code")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun DeterministicQrMatrix(payload: String, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, ParentOutline, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        val grid = 15
        val cell = size.width / grid
        val hash = payload.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isFinder = (r < 4 && c < 4) || (r < 4 && c >= grid - 4) || (r >= grid - 4 && c < 4)
                val bit = ((hash ushr ((r * 3 + c * 5) and 31)) xor (r * 7 + c * 11)) and 1 == 1
                if (isFinder || bit) {
                    drawRect(
                        color = ParentInkPrimary,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell * 0.9f, cell * 0.9f)
                    )
                }
            }
        }
    }
}
