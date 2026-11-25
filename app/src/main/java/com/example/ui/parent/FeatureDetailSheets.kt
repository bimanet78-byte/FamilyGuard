package com.example.ui.parent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FeatureCatalog
import com.example.data.model.ImplementationStatus
import com.example.data.model.ManagedDevice
import com.example.platform.DeviceTelemetryHelper
import com.example.platform.PermissionKind
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.CharcoalTextPrimary
import com.example.ui.theme.CharcoalTextSecondary
import com.example.ui.theme.ParentVioletPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.FamilyGuardViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusBadgeChip(label: String) {
    val color = when (label) {
        "IMPLEMENTED" -> StatusOnline
        "NEEDS_PERMISSION" -> StatusWarning
        "NEEDS_MANAGED_DEVICE" -> ParentVioletPrimary
        "PROTOTYPE_ONLY" -> Color(0xFF38BDF8)
        else -> StatusOffline
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FeatureMatrixDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Android API & Permission Matrix") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCatalog.capabilities.forEach { cap ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cap.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadgeChip(cap.status.label)
                        }
                        Text("API: ${cap.officialApi}", style = MaterialTheme.typography.bodyMedium)
                        Text("Permission: ${cap.requiredPermission}", style = MaterialTheme.typography.labelMedium)
                        Text("Consent: ${cap.consentFlow}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceFeatureSheetHost(
    sheet: ActiveFeatureSheet,
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        contentColor = CharcoalTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (sheet) {
                ActiveFeatureSheet.WALLPAPER -> WallpaperPickerContent(device, viewModel, onDismiss)
                ActiveFeatureSheet.LIVE_CAMERA -> CameraSnapshotContent(device, viewModel)
                ActiveFeatureSheet.SCREEN_SHARE -> ScreenShareContent(device, viewModel)
                ActiveFeatureSheet.LIVE_AUDIO -> LiveAudioContent(device, viewModel)
                ActiveFeatureSheet.LOCATION -> LocationShareContent(device, viewModel)
                ActiveFeatureSheet.GALLERY, ActiveFeatureSheet.SAVED_FILES -> GalleryPickerContent(viewModel)
                ActiveFeatureSheet.FILES -> FileManagerSafContent(viewModel)
                ActiveFeatureSheet.APPS, ActiveFeatureSheet.USAGE_ANALYTICS -> UsageAndAppsContent(device, viewModel)
                ActiveFeatureSheet.NOTIFICATIONS -> NotificationsContent(device, viewModel)
                ActiveFeatureSheet.SOUND_STATUS -> SoundStatusContent(device, viewModel)
                ActiveFeatureSheet.SESSION_LOGS -> AuditSessionsContent(viewModel)
                ActiveFeatureSheet.CALL_LOGS -> RestrictedPolicyContent(
                    title = "Call Logs & Blocking",
                    status = ImplementationStatus.NOT_SUPPORTED,
                    explanation = "Google Play restricts READ_CALL_LOG strictly to the user's default Phone / Dialer app (RoleManager.ROLE_DIALER). FamilyGuard does not bypass this system control or collect hidden call histories."
                )
                ActiveFeatureSheet.MESSAGES -> RestrictedPolicyContent(
                    title = "SMS & Messaging",
                    status = ImplementationStatus.NOT_SUPPORTED,
                    explanation = "SMS inbox access requires the default SMS handler role (RoleManager.ROLE_SMS). Private third-party chats (WhatsApp, IMO, Messenger, Gmail) are never intercepted or scraped."
                )
                ActiveFeatureSheet.CONTACTS -> ContactsPickerContent()
                ActiveFeatureSheet.UTILITIES -> UtilitiesOverviewContent(device, viewModel, onDismiss)
                ActiveFeatureSheet.SPEAK_MESSAGE -> CommandInputSheet(
                    title = "Speak Message (TTS)",
                    label = "Message to read aloud after child confirmation",
                    buttonText = "Send Speak Request",
                    onSubmit = { msg ->
                        viewModel.sendParentCommand(device, "SPEAK_MESSAGE", msg)
                        onDismiss()
                    }
                )
                ActiveFeatureSheet.SHOW_MESSAGE -> CommandInputSheet(
                    title = "Show Message in Kids Companion",
                    label = "Friendly message for child",
                    buttonText = "Send Message",
                    onSubmit = { msg ->
                        viewModel.sendParentCommand(device, "SHOW_MESSAGE", msg)
                        onDismiss()
                    }
                )
                ActiveFeatureSheet.EDIT_PIN -> CommandInputSheet(
                    title = "Update Kids Companion PIN",
                    label = "4-8 digit security PIN (stored as SHA-256 hash)",
                    buttonText = "Save Security PIN",
                    onSubmit = { pin ->
                        if (pin.length in 4..8) {
                            viewModel.updateParentSettings(device.id, newPin = pin)
                            viewModel.showMessage("Updated security PIN hash")
                            onDismiss()
                        } else {
                            viewModel.showMessage("PIN must be 4-8 digits")
                        }
                    }
                )
                ActiveFeatureSheet.EDIT_NAME -> CommandInputSheet(
                    title = "Edit Kid's Display Name",
                    label = "Child / Device Nickname",
                    initialValue = device.kidName,
                    buttonText = "Save Name",
                    onSubmit = { name ->
                        viewModel.updateParentSettings(device.id, kidName = name)
                        onDismiss()
                    }
                )
                ActiveFeatureSheet.PERMISSIONS -> DevicePermissionsContent(viewModel)
                ActiveFeatureSheet.LOCK_DEVICE_INFO -> LockDeviceManagedContent(device, viewModel)
            }
        }
    }
}

@Composable
private fun WallpaperPickerContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel,
    onDismiss: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.addGalleryUris(listOf(uri))
            viewModel.showMessage("Selected custom image via Photo Picker; child confirms WallpaperManager apply.")
        }
    }

    Text("Kids Companion Wallpaper Preview", style = MaterialTheme.typography.titleLarge)
    StatusBadgeChip("IMPLEMENTED")
    Text(
        "Wallpapers apply only after child confirmation in Kids Companion — never silently.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "starstreak" to Pair("Starstreak Dark", R.drawable.img_wallpaper_starstreak),
            "violet_aurora" to Pair("Violet Aurora", R.drawable.img_wallpaper_violet_aurora)
        ).forEach { (id, info) ->
            val selected = device.wallpaperId == id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) ParentVioletPrimary else CharcoalBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable {
                        viewModel.updateParentSettings(device.id, wallpaperId = id)
                        viewModel.sendParentCommand(device, "SET_WALLPAPER", id)
                        onDismiss()
                    }
                    .padding(10.dp)
            ) {
                Image(
                    painter = painterResource(id = info.second),
                    contentDescription = info.first,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.65f)
                        .clip(RoundedCornerShape(12.dp))
                )
                Text(info.first, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    OutlinedButton(
        onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Preview Custom Photo via Photo Picker")
    }
}

@Composable
private fun CameraSnapshotContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var useFront by remember { mutableStateOf(false) }
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPerm = granted
        viewModel.refreshLocalDeviceDiagnostics(context)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Consent-Gated Camera Snapshot", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(if (hasCameraPerm) "IMPLEMENTED" else "NEEDS_PERMISSION")
    }
    Text(
        "The child must explicitly approve each camera session on their device. Camera never starts secretly.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = {
                useFront = true
                viewModel.sendParentCommand(device, "REQUEST_CAMERA_FRONT", "Requesting front camera snapshot")
            },
            modifier = Modifier.weight(1f)
        ) { Text("Request Front") }
        FilledTonalButton(
            onClick = {
                useFront = false
                viewModel.sendParentCommand(device, "REQUEST_CAMERA_BACK", "Requesting back camera snapshot")
            },
            modifier = Modifier.weight(1f)
        ) { Text("Request Back") }
    }

    if (device.activeCameraSession && hasCameraPerm) {
        Surface(
            color = StatusOnline.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LIVE SESSION APPROVED BY CHILD", color = StatusOnline, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { viewModel.stopActiveSession(context, device, "camera") }) {
                    Text("Stop", color = StatusError)
                }
            }
        }
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview)
                    } catch (_: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    } else if (!hasCameraPerm) {
        Button(
            onClick = { camLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Grant Camera Permission on This Device")
        }
    } else {
        Surface(
            color = CharcoalSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Camera preview is idle. Send a Front or Back request above and approve it inside Kids Companion.",
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalTextSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun ScreenShareContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Screen Snapshot / Sharing", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip("NEEDS_PERMISSION")
    }
    Text(
        "Uses Android MediaProjectionManager.createScreenCaptureIntent(), which always shows Android's official system consent dialog and a persistent active-session notification on the child's device.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    if (device.activeScreenShareSession) {
        Surface(
            color = StatusOnline.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Screen sharing active (consent granted)", color = StatusOnline)
                Button(
                    onClick = { viewModel.stopActiveSession(context, device, "screen") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) { Text("Stop") }
            }
        }
    } else {
        Button(
            onClick = { viewModel.sendParentCommand(device, "REQUEST_SCREEN_SHARE", "Parent requested consented screen share") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.ScreenShare, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Request Child Consent for Screen Share")
        }
    }
}

@Composable
private fun LiveAudioContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel
) {
    val context = LocalContext.current
    var hasAudio by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudio = granted
        viewModel.refreshLocalDeviceDiagnostics(context)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Live Audio Session", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(if (hasAudio) "IMPLEMENTED" else "NEEDS_PERMISSION")
    }
    Text(
        "Supports only explicitly child-approved sessions with an active microphone indicator and immediate Stop control.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    if (!hasAudio) {
        OutlinedButton(
            onClick = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Grant Microphone Permission") }
    }
    if (device.activeAudioSession) {
        Button(
            onClick = { viewModel.stopActiveSession(context, device, "audio") },
            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.StopCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Stop Active Audio Session")
        }
    } else {
        Button(
            onClick = { viewModel.sendParentCommand(device, "REQUEST_LIVE_AUDIO", "Parent requested live audio check-in") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Request Child Audio Check-In") }
    }
}

@Composable
private fun LocationShareContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel
) {
    val context = LocalContext.current
    val locationsState by viewModel.locationSharesState.collectAsStateWithLifecycle()
    val locations = (locationsState as? UiState.Success)?.data.orEmpty()
    val latest = locations.firstOrNull()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Locate Phone & Location", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(if (device.activeLocationSession) "IMPLEMENTED" else "NEEDS_PERMISSION")
    }
    Text(
        "Location coordinates, accuracy, and timestamps synchronize only while Location Sharing is actively enabled by the child.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )

    if (latest != null && latest.active) {
        val ts = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()).format(latest.updatedAt?.toDate() ?: Date())
        Surface(
            color = CharcoalSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Lat: ${"%.5f".format(latest.latitude)}  Lng: ${"%.5f".format(latest.longitude)}", style = MaterialTheme.typography.titleMedium)
                Text("Accuracy: ±${latest.accuracyMeters.toInt()} m • Provider: ${latest.provider}", color = CharcoalTextSecondary)
                Text("Updated: $ts", color = StatusOnline, style = MaterialTheme.typography.labelMedium)
            }
        }
    } else {
        Text("Location sharing is currently paused.", color = CharcoalTextSecondary)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { viewModel.sendParentCommand(device, "REQUEST_LOCATION", "Parent requested live location share") },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Request Location")
        }
        if (device.activeLocationSession) {
            OutlinedButton(
                onClick = { viewModel.stopActiveSession(context, device, "location") },
                modifier = Modifier.weight(1f)
            ) { Text("Stop Sharing") }
        }
    }
}

@Composable
private fun GalleryPickerContent(viewModel: FamilyGuardViewModel) {
    val uris by viewModel.selectedGalleryUris.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { picked ->
        if (picked.isNotEmpty()) viewModel.addGalleryUris(picked)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Authorized Gallery (Photo Picker)", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip("IMPLEMENTED")
    }
    Text(
        "Uses Google Play compliant zero-permission Android Photo Picker (`PickVisualMedia`).",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    Button(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.PermMedia, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Select Photos or Videos")
    }
    uris.forEach { uri ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CharcoalSurfaceElevated)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected media preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text(uri.lastPathSegment ?: "Media item", maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { viewModel.removeGalleryUri(uri) }) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove from list", tint = CharcoalTextSecondary)
            }
        }
    }
}

@Composable
private fun FileManagerSafContent(viewModel: FamilyGuardViewModel) {
    val context = LocalContext.current
    val files by viewModel.selectedSafFiles.collectAsStateWithLifecycle()
    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { viewModel.addSafFile(context, it) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("File Manager (Storage Access Framework)", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip("IMPLEMENTED")
    }
    Text(
        "Accesses only user-selected documents, downloads, audio, and videos via Android SAF without broad storage permissions.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    Button(
        onClick = { docLauncher.launch(arrayOf("*/*")) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Browse User-Selected Files")
    }
    files.forEach { meta ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CharcoalSurfaceElevated)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meta.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${meta.mimeType} • ${meta.sizeBytes / 1024} KB", style = MaterialTheme.typography.bodyMedium, color = CharcoalTextSecondary)
            }
            IconButton(onClick = { viewModel.removeSafFile(meta.uri) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = CharcoalTextSecondary)
            }
        }
    }
}

@Composable
private fun UsageAndAppsContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel
) {
    val context = LocalContext.current
    val usageList by viewModel.usageStats.collectAsStateWithLifecycle()
    val rulesState by viewModel.rulesState.collectAsStateWithLifecycle()
    val rules = (rulesState as? UiState.Success)?.data.orEmpty()
    val hasUsage = remember(usageList) { DeviceTelemetryHelper.hasUsageStatsPermission(context) }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalDeviceDiagnostics(context)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Usage Analytics & App Rules", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(if (hasUsage) "IMPLEMENTED" else "NEEDS_PERMISSION")
    }
    Text(
        "Powered by UsageStatsManager (usage data is refreshed periodically by Android and may not be instantaneous). Universal app pause requires Managed Device / Family Link enrollment.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )

    if (!hasUsage) {
        Button(
            onClick = { DeviceTelemetryHelper.openUsageAccessSettings(context) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Grant Usage Access in Android Settings") }
    }

    val maxMins = (usageList.maxOfOrNull { it.totalMinutesToday } ?: 60).coerceAtLeast(1)
    usageList.take(8).forEach { app ->
        val existingRule = rules.firstOrNull { it.packageName == app.packageName }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CharcoalSurfaceElevated)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium)
                    Text("${app.totalMinutesToday} min today • ${app.packageName}", style = MaterialTheme.typography.bodyMedium, color = CharcoalTextSecondary)
                }
                Switch(
                    checked = existingRule?.paused == true,
                    onCheckedChange = { paused ->
                        viewModel.upsertScreenTimeRule(
                            device = device,
                            packageName = app.packageName,
                            appName = app.appName,
                            dailyLimitMinutes = existingRule?.dailyLimitMinutes?.toInt() ?: 60,
                            paused = paused,
                            scheduleLabel = existingRule?.scheduleLabel?.ifBlank { "Daily 60m limit" } ?: "Daily 60m limit"
                        )
                    }
                )
            }
            LinearProgressIndicator(
                progress = { (app.totalMinutesToday.toFloat() / maxMins.toFloat()).coerceIn(0.05f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = ParentVioletPrimary
            )
        }
    }

    if (rules.isNotEmpty()) {
        Text("Synced Screen-Time Schedules", style = MaterialTheme.typography.titleMedium)
        rules.forEach { rule ->
            Text("• ${rule.appName}: ${rule.dailyLimitMinutes}m (${if (rule.paused) "Paused" else "Active"})", color = CharcoalTextSecondary)
        }
    }
}

@Composable
private fun NotificationsContent(device: ManagedDevice, viewModel: FamilyGuardViewModel) {
    val context = LocalContext.current
    val enabled = remember { DeviceTelemetryHelper.hasNotificationListenerAccess(context) }
    val logsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val logs = (logsState as? UiState.Success)?.data.orEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Notifications & Listener Status", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(if (enabled) "IMPLEMENTED" else "NEEDS_PERMISSION")
    }
    Text(
        "Notification history requires explicit Notification Access in Android Settings and respects system redaction for sensitive content.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    if (!enabled) {
        OutlinedButton(
            onClick = { DeviceTelemetryHelper.openNotificationListenerSettings(context) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open Notification Access Settings") }
    }
    logs.take(5).forEach { log ->
        Text("• ${log.action}: ${log.details}", style = MaterialTheme.typography.bodyMedium, color = CharcoalTextSecondary)
    }
}

@Composable
private fun SoundStatusContent(device: ManagedDevice, viewModel: FamilyGuardViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Sound & Device Status", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip("IMPLEMENTED")
    }
    Text("Ringer Mode: ${device.ringerMode.uppercase()} • Volume: ${device.volumePercent}%", color = CharcoalTextSecondary)
    Button(
        onClick = { viewModel.sendParentCommand(device, "LOCATE_SOUND", "Play locate ringtone") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Ring Device to Locate")
    }
}

@Composable
private fun AuditSessionsContent(viewModel: FamilyGuardViewModel) {
    val logsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val logs = (logsState as? UiState.Success)?.data.orEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Transparent Session Audit Log", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip("IMPLEMENTED")
    }
    if (logs.isEmpty()) {
        Text("No session events logged yet.", color = CharcoalTextSecondary)
    } else {
        logs.take(15).forEach { log ->
            Surface(
                color = CharcoalSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${log.action} (${log.status})", style = MaterialTheme.typography.titleMedium)
                    Text(log.details, style = MaterialTheme.typography.bodyMedium, color = CharcoalTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RestrictedPolicyContent(
    title: String,
    status: ImplementationStatus,
    explanation: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(status.label)
    }
    Surface(
        color = CharcoalSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = StatusWarning)
            Text(explanation, style = MaterialTheme.typography.bodyMedium, color = CharcoalTextSecondary)
        }
    }
}

@Composable
private fun ContactsPickerContent() {
    val context = LocalContext.current
    var pickedContactUri by remember { mutableStateOf<Uri?>(null) }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        pickedContactUri = uri
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Contacts (Zero-Permission Picker)", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip("IMPLEMENTED")
    }
    Text(
        "Uses Android's system Contact Picker without requesting broad READ_CONTACTS permission.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { contactPicker.launch(null) }, modifier = Modifier.weight(1f)) {
            Text("Pick Contact")
        }
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Contact")
        }
    }
    pickedContactUri?.let {
        Text("Selected contact URI: $it", style = MaterialTheme.typography.bodyMedium, color = StatusOnline)
    }
}

@Composable
private fun UtilitiesOverviewContent(
    device: ManagedDevice,
    viewModel: FamilyGuardViewModel,
    onDismiss: () -> Unit
) {
    Text("Quick Device Utilities", style = MaterialTheme.typography.titleLarge)
    Button(
        onClick = {
            viewModel.sendParentCommand(device, "VIBRATE", "Parent sent vibration alert")
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Vibration, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Vibrate Phone")
    }
    OutlinedButton(
        onClick = {
            viewModel.sendParentCommand(device, "LOCATE_SOUND", "Parent triggered locate sound")
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Play Locate Chime")
    }
}

@Composable
private fun CommandInputSheet(
    title: String,
    label: String,
    buttonText: String,
    initialValue: String = "",
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    Text(title, style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = { if (text.isNotBlank()) onSubmit(text.trim()) },
        enabled = text.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(buttonText)
    }
}

@Composable
private fun DevicePermissionsContent(viewModel: FamilyGuardViewModel) {
    val context = LocalContext.current
    val perms by viewModel.permissionStatuses.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshLocalDeviceDiagnostics(context) }

    Text("Device Permissions & Roles", style = MaterialTheme.typography.titleLarge)
    perms.forEach { perm ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CharcoalSurfaceElevated)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(perm.title, style = MaterialTheme.typography.titleMedium)
                Text(perm.description, style = MaterialTheme.typography.bodyMedium, color = CharcoalTextSecondary)
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
                }) { Text("Configure") }
            }
        }
    }
}

@Composable
private fun LockDeviceManagedContent(device: ManagedDevice, viewModel: FamilyGuardViewModel) {
    val context = LocalContext.current
    val isAdmin = remember { DeviceTelemetryHelper.isDeviceAdminActive(context) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Lock Device (Managed Device)", style = MaterialTheme.typography.titleLarge)
        StatusBadgeChip(if (isAdmin) "IMPLEMENTED" else "NEEDS_MANAGED_DEVICE")
    }
    Text(
        "Remote device locking requires explicit DevicePolicyManager admin / Managed Device enrollment on the child phone. Ordinary apps cannot lock a device without Android system authorization.",
        style = MaterialTheme.typography.bodyMedium,
        color = CharcoalTextSecondary
    )
    Button(
        onClick = { viewModel.sendParentCommand(device, "LOCK_DEVICE", "Requested managed device lock") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Log Managed Lock Request")
    }
}
