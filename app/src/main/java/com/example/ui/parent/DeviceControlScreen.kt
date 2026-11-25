package com.example.ui.parent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhotoCameraBack
import androidx.compose.material.icons.outlined.PhotoCameraFront
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ManagedDevice
import com.example.platform.DeviceTelemetryHelper
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalDangerBorder
import com.example.ui.theme.CharcoalDangerContainer
import com.example.ui.theme.CharcoalDangerText
import com.example.ui.theme.CharcoalPill
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.CharcoalTextPrimary
import com.example.ui.theme.CharcoalTextSecondary
import com.example.ui.theme.FamilyGuardTheme
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.viewmodel.FamilyGuardViewModel
import kotlinx.coroutines.launch

enum class ActiveFeatureSheet {
    WALLPAPER,
    SCREEN_SHARE,
    LIVE_CAMERA,
    LIVE_AUDIO,
    LOCATION,
    SAVED_FILES,
    NOTIFICATIONS,
    SOUND_STATUS,
    SESSION_LOGS,
    CALL_LOGS,
    CONTACTS,
    MESSAGES,
    GALLERY,
    FILES,
    APPS,
    UTILITIES,
    USAGE_ANALYTICS,
    SPEAK_MESSAGE,
    SHOW_MESSAGE,
    EDIT_PIN,
    EDIT_NAME,
    PERMISSIONS,
    LOCK_DEVICE_INFO
}

@Composable
fun DeviceControlScreen(
    viewModel: FamilyGuardViewModel,
    deviceId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val devicesState by viewModel.devicesState.collectAsStateWithLifecycle()
    val devices = (devicesState as? com.example.ui.viewmodel.UiState.Success)?.data.orEmpty()
    val device = devices.firstOrNull { it.id == deviceId } ?: devices.firstOrNull()

    var activeSheet by remember { mutableStateOf<ActiveFeatureSheet?>(null) }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    FamilyGuardTheme(darkTheme = true) {
        Scaffold(
            containerColor = CharcoalBackground
        ) { innerPadding ->
            if (device == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Device not found or unlinked", color = CharcoalTextPrimary)
                        Button(onClick = onBack) { Text("Back to Dashboard") }
                    }
                }
                return@Scaffold
            }

            // Local responsive brightness & volume sliders synced with Firestore
            var brightness by remember(device.brightnessPercent) { mutableIntStateOf(device.brightnessPercent.toInt()) }
            var volume by remember(device.volumePercent) { mutableIntStateOf(device.volumePercent.toInt()) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF282B33), CharcoalBackground),
                            radius = 1200f
                        )
                    )
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TOP BAR (Exact Match to Screenshots 2-7)
                    item {
                        DeviceControlTopBar(
                            deviceName = device.deviceName,
                            batteryPercent = device.batteryPercent.toInt(),
                            connectionState = device.connectionState,
                            onBack = onBack
                        )
                    }

                    // UPPER DEVICE HERO: Realistic Smartphone Mockup (left) + Telemetry Metadata (right)
                    item {
                        DeviceHeroOverviewRow(
                            device = device,
                            onMockupTapped = { activeSheet = ActiveFeatureSheet.WALLPAPER }
                        )
                    }

                    // SWIPEABLE 2-PAGE FEATURE DECK (Screenshots 2 & 4!)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                pageSpacing = 16.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("device_control_pager")
                            ) { page ->
                                if (page == 0) {
                                    // PAGE 1: Matches Screenshots 2 & 7 (4x2 dark rounded tiles)
                                    SwipePageOneGrid(
                                        device = device,
                                        onOpenSheet = { activeSheet = it }
                                    )
                                } else {
                                    // PAGE 2: Matches Screenshot 4 (Left 2x3 tiles + Right 2 top tiles & 2 tall vertical sliders)
                                    SwipePageTwoGrid(
                                        brightness = brightness,
                                        volume = volume,
                                        onBrightnessChange = { newVal ->
                                            brightness = newVal
                                            DeviceTelemetryHelper.setSystemBrightnessPercent(context, newVal)
                                        },
                                        onBrightnessCommit = {
                                            viewModel.updateParentSettings(device.id, brightnessPercent = brightness)
                                        },
                                        onVolumeChange = { newVal ->
                                            volume = newVal
                                            DeviceTelemetryHelper.setMediaVolumePercent(context, newVal)
                                        },
                                        onVolumeCommit = {
                                            viewModel.updateParentSettings(device.id, volumePercent = volume)
                                        },
                                        onOpenSheet = { activeSheet = it }
                                    )
                                }
                            }

                            // Interactive Swipe Page Indicator Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("Live & Media", "Tools & Sliders").forEachIndexed { idx, label ->
                                    val selected = pagerState.currentPage == idx
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) CharcoalPill else Color.Transparent,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                scope.launch { pagerState.animateScrollToPage(idx) }
                                            }
                                            .testTag("swipe_tab_$idx")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (selected) Color.White else CharcoalTextSecondary.copy(alpha = 0.45f))
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) CharcoalTextPrimary else CharcoalTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // LOWER CARDS SECTION (Exact match to Screenshots 3, 5, 6!)
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Camera Snapshot Card
                            DarkSectionCard(
                                headerIcon = Icons.Outlined.CameraAlt,
                                title = "Camera Snapshot"
                            ) {
                                DarkActionRow(
                                    icon = Icons.Outlined.PhotoCameraFront,
                                    title = "Front Camera",
                                    subtitle = "Capture from front camera (child-approved)",
                                    onClick = { activeSheet = ActiveFeatureSheet.LIVE_CAMERA }
                                )
                                DarkActionRow(
                                    icon = Icons.Outlined.PhotoCameraBack,
                                    title = "Back Camera",
                                    subtitle = "Capture from back camera (child-approved)",
                                    onClick = { activeSheet = ActiveFeatureSheet.LIVE_CAMERA }
                                )
                            }

                            // 2. Screen Snapshot Card
                            DarkSectionCard(
                                headerIcon = Icons.Outlined.PhoneAndroid,
                                title = "Screen Snapshot"
                            ) {
                                DarkActionRow(
                                    icon = Icons.Outlined.Screenshot,
                                    title = "Screenshot",
                                    subtitle = "Capture the device screen via MediaProjection",
                                    onClick = { activeSheet = ActiveFeatureSheet.SCREEN_SHARE }
                                )
                            }

                            // 3. Device Utilities Card
                            DarkSectionCard(
                                headerIcon = Icons.Outlined.Tune,
                                title = "Device Utilities"
                            ) {
                                DarkActionRow(
                                    icon = Icons.Rounded.RecordVoiceOver,
                                    title = "Speak Message",
                                    subtitle = "Text-to-speech on device",
                                    onClick = { activeSheet = ActiveFeatureSheet.SPEAK_MESSAGE }
                                )
                                DarkActionRow(
                                    icon = Icons.Outlined.Message,
                                    title = "Show Message",
                                    subtitle = "Display message on screen",
                                    onClick = { activeSheet = ActiveFeatureSheet.SHOW_MESSAGE }
                                )
                                DarkActionRow(
                                    icon = Icons.Outlined.Vibration,
                                    title = "Vibrate Phone",
                                    subtitle = "Trigger device vibration",
                                    onClick = {
                                        viewModel.sendParentCommand(device, "VIBRATE", "Parent triggered locate vibration")
                                    }
                                )
                                DarkActionRow(
                                    icon = Icons.Rounded.GpsFixed,
                                    title = "Locate Phone",
                                    subtitle = "Play sound & share location while enabled",
                                    onClick = { activeSheet = ActiveFeatureSheet.LOCATION }
                                )
                                DarkActionRow(
                                    icon = Icons.Outlined.Lock,
                                    title = "Lock Device",
                                    subtitle = "Lock the device remotely (Managed Device)",
                                    onClick = { activeSheet = ActiveFeatureSheet.LOCK_DEVICE_INFO }
                                )
                            }

                            // 4. Device Settings Card
                            DarkSectionCard(
                                headerIcon = Icons.Outlined.Settings,
                                title = "Device Settings"
                            ) {
                                DarkActionRow(
                                    icon = Icons.Rounded.Password,
                                    title = "Edit Kid's Pin",
                                    subtitle = "Change app security pin",
                                    onClick = { activeSheet = ActiveFeatureSheet.EDIT_PIN }
                                )
                                DarkActionRow(
                                    icon = Icons.Rounded.Edit,
                                    title = "Edit Kid's Name",
                                    subtitle = "Change device display name",
                                    onClick = { activeSheet = ActiveFeatureSheet.EDIT_NAME }
                                )
                                DarkActionRow(
                                    icon = Icons.Outlined.Tune,
                                    title = "Device Permissions",
                                    subtitle = "View and manage permissions (${device.permissionsGrantedCount}/${device.permissionsTotalCount})",
                                    onClick = { activeSheet = ActiveFeatureSheet.PERMISSIONS }
                                )
                            }

                            // 5. Danger Zone Card (Matches Screenshots 5 & 6)
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = CharcoalDangerContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalDangerBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = null,
                                            tint = CharcoalDangerText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Danger Zone",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CharcoalDangerText
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { showUnlinkDialog = true }
                                            .padding(vertical = 8.dp)
                                            .testTag("unlink_device_btn"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(CharcoalDangerText.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = CharcoalDangerText
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Unlink Kid's Device",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = CharcoalDangerText
                                                )
                                                Text(
                                                    text = "Removes control over the device",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = CharcoalDangerText.copy(alpha = 0.78f)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = CharcoalDangerText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            activeSheet?.let { sheet ->
                DeviceFeatureSheetHost(
                    sheet = sheet,
                    device = device,
                    viewModel = viewModel,
                    onDismiss = { activeSheet = null }
                )
            }

            if (showUnlinkDialog) {
                AlertDialog(
                    onDismissRequest = { showUnlinkDialog = false },
                    containerColor = CharcoalSurface,
                    titleContentColor = CharcoalTextPrimary,
                    textContentColor = CharcoalTextSecondary,
                    title = { Text("Unlink ${device.kidName}?") },
                    text = {
                        Text("This transparently revokes parent supervision and disconnects ${device.deviceName} from your FamilyGuard list.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showUnlinkDialog = false
                                viewModel.unlinkDevice(device.id)
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalDangerText)
                        ) {
                            Text("Unlink Device")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnlinkDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceControlTopBar(
    deviceName: String,
    batteryPercent: Int,
    connectionState: String,
    onBack: () -> Unit
) {
    val dotColor = when (connectionState) {
        "online" -> StatusOnline
        "connecting" -> StatusConnecting
        else -> StatusOffline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_to_dashboard")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = CharcoalTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = CharcoalTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$batteryPercent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalTextSecondary
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = CharcoalPill,
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = connectionState.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CharcoalTextPrimary
                )
            }
        }
    }
}

@Composable
private fun DeviceHeroOverviewRow(
    device: ManagedDevice,
    onMockupTapped: () -> Unit
) {
    val wallpaperRes = when (device.wallpaperId) {
        "violet_aurora" -> R.drawable.img_wallpaper_violet_aurora
        else -> R.drawable.img_wallpaper_starstreak
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Realistic Smartphone Mockup matching Screenshots 2 & 4
        Box(
            modifier = Modifier
                .weight(0.45f)
                .aspectRatio(0.49f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0A0B0D))
                .border(3.dp, Color(0xFFA0A4AE), RoundedCornerShape(28.dp))
                .clickable(onClick = onMockupTapped)
                .padding(5.dp)
                .testTag("phone_mockup_preview")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(23.dp))
            ) {
                Image(
                    painter = painterResource(id = wallpaperRes),
                    contentDescription = "Child phone wallpaper preview. Tap to customize.",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dynamic Island / Camera Cutout
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .width(44.dp)
                        .height(11.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black)
                )
                // Subtle wallpaper tap hint at bottom
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "Tap wallpaper",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Right: Telemetry Specs Column matching Screenshots 2 & 4
        Column(
            modifier = Modifier.weight(0.55f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TelemetryRow(label = "DEVICE NAME", value = device.deviceName)
            TelemetryRow(label = "KID'S VERSION", value = device.kidsAppVersion)
            TelemetryRow(label = "MANUFACTURER", value = device.manufacturer)
            TelemetryRow(label = "ANDROID VERSION", value = device.androidVersion)
            TelemetryRow(label = "NETWORK INFO", value = device.networkType.ifBlank { "-" })
            TelemetryRow(
                label = "BATTERY INFO",
                value = buildString {
                    append("${device.batteryPercent}%")
                    append("\n${device.batteryTempC}°C")
                    append("\n${if (device.batteryCharging) "Charging" else "Discharging"}")
                    append("\n${device.batteryCurrentMa} mA")
                },
                showDivider = false
            )
        }
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = CharcoalTextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = CharcoalTextSecondary
        )
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.7.dp,
                color = CharcoalBorder.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SwipePageOneGrid(
    device: ManagedDevice,
    onOpenSheet: (ActiveFeatureSheet) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DarkShortcutTile(
                icon = Icons.Outlined.PhoneAndroid,
                label = "Screen Share",
                activeDot = device.activeScreenShareSession,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.SCREEN_SHARE) }
            )
            DarkShortcutTile(
                icon = Icons.Outlined.CameraAlt,
                label = "Live Camera",
                activeDot = device.activeCameraSession,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.LIVE_CAMERA) }
            )
            DarkShortcutTile(
                icon = Icons.Outlined.Mic,
                label = "Live Audio",
                activeDot = device.activeAudioSession,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.LIVE_AUDIO) }
            )
            DarkShortcutTile(
                icon = Icons.Outlined.LocationOn,
                label = "Location",
                activeDot = device.activeLocationSession,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.LOCATION) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DarkShortcutTile(
                icon = Icons.Outlined.CloudDownload,
                label = "Saved Files",
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.SAVED_FILES) }
            )
            DarkShortcutTile(
                icon = Icons.Outlined.Notifications,
                label = "Notifications",
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.NOTIFICATIONS) }
            )
            DarkShortcutTile(
                icon = Icons.Outlined.NotificationsActive,
                label = "Sound",
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.SOUND_STATUS) }
            )
            DarkShortcutTile(
                icon = Icons.Outlined.RadioButtonChecked,
                label = "Sessions",
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(ActiveFeatureSheet.SESSION_LOGS) }
            )
        }
    }
}

/**
 * Matches Screenshot 4: Left 2x3 tiles (Call Logs, Contacts, Messages, Gallery, Files, Apps)
 * + Right top row (Utilities, Usage) + Right bottom 2 tall vertical pill sliders (Brightness, Volume).
 */
@Composable
private fun SwipePageTwoGrid(
    brightness: Int,
    volume: Int,
    onBrightnessChange: (Int) -> Unit,
    onBrightnessCommit: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onVolumeCommit: () -> Unit,
    onOpenSheet: (ActiveFeatureSheet) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left 2 columns x 3 rows
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DarkShortcutTile(
                    icon = Icons.Outlined.Call,
                    label = "Call Logs",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.CALL_LOGS) }
                )
                DarkShortcutTile(
                    icon = Icons.Outlined.Contacts,
                    label = "Contacts",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.CONTACTS) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DarkShortcutTile(
                    icon = Icons.Outlined.Message,
                    label = "Messages",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.MESSAGES) }
                )
                DarkShortcutTile(
                    icon = Icons.Outlined.PermMedia,
                    label = "Gallery",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.GALLERY) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DarkShortcutTile(
                    icon = Icons.Outlined.Folder,
                    label = "Files",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.FILES) }
                )
                DarkShortcutTile(
                    icon = Icons.Outlined.Apps,
                    label = "Apps",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.APPS) }
                )
            }
        }

        // Right 2 columns: Top row (Utilities, Usage) + Tall vertical Brightness & Volume sliders
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DarkShortcutTile(
                    icon = Icons.Outlined.Tune,
                    label = "Utilities",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.UTILITIES) }
                )
                DarkShortcutTile(
                    icon = Icons.Outlined.Timer,
                    label = "Usage",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet(ActiveFeatureSheet.USAGE_ANALYTICS) }
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                VerticalPillSlider(
                    valuePercent = brightness,
                    icon = Icons.Rounded.WbSunny,
                    contentDesc = "Brightness Slider",
                    onValueChange = onBrightnessChange,
                    onChangeFinished = onBrightnessCommit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("brightness_vertical_slider")
                )
                VerticalPillSlider(
                    valuePercent = volume,
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDesc = "Volume Slider",
                    onValueChange = onVolumeChange,
                    onChangeFinished = onVolumeCommit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("volume_vertical_slider")
                )
            }
        }
    }
}

@Composable
private fun DarkShortcutTile(
    icon: ImageVector,
    label: String,
    activeDot: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CharcoalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder.copy(alpha = 0.7f)),
        modifier = modifier
            .height(81.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("tile_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (activeDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StatusOnline)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = CharcoalTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                    color = CharcoalTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VerticalPillSlider(
    valuePercent: Int,
    icon: ImageVector,
    contentDesc: String,
    onValueChange: (Int) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableIntStateOf(1) }
    val fraction = (valuePercent.coerceIn(0, 100)) / 100f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CharcoalSurface)
            .border(1.dp, CharcoalBorder.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pct = (((heightPx - offset.y) / heightPx) * 100f).toInt().coerceIn(0, 100)
                    onValueChange(pct)
                    onChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onChangeFinished() },
                    onDragCancel = { onChangeFinished() }
                ) { change, _ ->
                    change.consume()
                    val pct = (((heightPx - change.position.y) / heightPx) * 100f).toInt().coerceIn(0, 100)
                    onValueChange(pct)
                }
            }
    ) {
        // Fill bar rising from bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .background(CharcoalSurfaceElevated)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$valuePercent%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = CharcoalTextSecondary
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = CharcoalTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DarkSectionCard(
    headerIcon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CharcoalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = CharcoalTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CharcoalTextPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun DarkActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
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
                    .background(CharcoalSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CharcoalTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CharcoalTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = CharcoalTextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = CharcoalTextSecondary
        )
    }
}
