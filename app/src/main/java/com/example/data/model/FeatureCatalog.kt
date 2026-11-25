package com.example.data.model

enum class ImplementationStatus(val label: String) {
    IMPLEMENTED("IMPLEMENTED"),
    NEEDS_PERMISSION("NEEDS_PERMISSION"),
    NEEDS_MANAGED_DEVICE("NEEDS_MANAGED_DEVICE"),
    PROTOTYPE_ONLY("PROTOTYPE_ONLY"),
    NOT_SUPPORTED("NOT_SUPPORTED")
}

data class FeatureCapabilitySpec(
    val id: String,
    val title: String,
    val status: ImplementationStatus,
    val requiredPermission: String,
    val officialApi: String,
    val roleOrManagedNote: String,
    val consentFlow: String
)

object FeatureCatalog {
    val capabilities: List<FeatureCapabilitySpec> = listOf(
        FeatureCapabilitySpec(
            id = "dashboard_telemetry",
            title = "Device Overview & Telemetry",
            status = ImplementationStatus.IMPLEMENTED,
            requiredPermission = "ACCESS_NETWORK_STATE (Install-time)",
            officialApi = "BatteryManager, ConnectivityManager, Build, Cloud Firestore",
            roleOrManagedNote = "No special role required",
            consentFlow = "Child pairs device via short-lived code and sees live sync status."
        ),
        FeatureCapabilitySpec(
            id = "wallpaper_preview",
            title = "Wallpaper Preview & Selection",
            status = ImplementationStatus.IMPLEMENTED,
            requiredPermission = "None (Photo Picker / user-confirmed apply)",
            officialApi = "PickVisualMedia + WallpaperManager (user-confirmed)",
            roleOrManagedNote = "Never changes system wallpaper silently",
            consentFlow = "Parent proposes wallpaper; child confirms applying on their device."
        ),
        FeatureCapabilitySpec(
            id = "screen_sharing",
            title = "Screen Snapshot / Sharing",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "POST_NOTIFICATIONS + MediaProjection system consent",
            officialApi = "MediaProjectionManager.createScreenCaptureIntent()",
            roleOrManagedNote = "Requires foregroundServiceType=mediaProjection & per-session dialog",
            consentFlow = "Child sees system consent dialog, persistent indicator, and Stop button."
        ),
        FeatureCapabilitySpec(
            id = "live_camera",
            title = "Camera Snapshot (Front / Back)",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "CAMERA (Runtime)",
            officialApi = "CameraX ProcessCameraProvider + ImageCapture",
            roleOrManagedNote = "Standard runtime camera permission; no hidden capture",
            consentFlow = "Child explicitly approves each camera session and can stop anytime."
        ),
        FeatureCapabilitySpec(
            id = "live_audio",
            title = "Live Audio Session",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "RECORD_AUDIO (Runtime)",
            officialApi = "AudioRecord / MediaRecorder with active indicator",
            roleOrManagedNote = "Foreground indicator mandatory on Android 14+",
            consentFlow = "Child explicitly approves microphone session and sees duration + Stop."
        ),
        FeatureCapabilitySpec(
            id = "location_share",
            title = "Locate Phone & Location Sharing",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION",
            officialApi = "FusedLocationProviderClient",
            roleOrManagedNote = "Play policy requires prominent disclosure before location prompt",
            consentFlow = "Shared only while activeLocationSession is enabled by child."
        ),
        FeatureCapabilitySpec(
            id = "gallery_picker",
            title = "Gallery (Zero-Permission Picker)",
            status = ImplementationStatus.IMPLEMENTED,
            requiredPermission = "None (Android Photo Picker)",
            officialApi = "ActivityResultContracts.PickVisualMedia",
            roleOrManagedNote = "Compliant with Google Play Photo & Video Permissions policy",
            consentFlow = "User explicitly selects photos/videos to inspect or share."
        ),
        FeatureCapabilitySpec(
            id = "file_manager",
            title = "File Manager (SAF)",
            status = ImplementationStatus.IMPLEMENTED,
            requiredPermission = "None (Storage Access Framework)",
            officialApi = "ActivityResultContracts.OpenDocument / OpenDocumentTree",
            roleOrManagedNote = "Scoped to user-chosen documents/folders only",
            consentFlow = "Displays name, MIME type, size; deletion requires confirmation."
        ),
        FeatureCapabilitySpec(
            id = "usage_analytics",
            title = "Usage Analytics & Floating Panel",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "PACKAGE_USAGE_STATS (Special App Access)",
            officialApi = "UsageStatsManager + AppOpsManager",
            roleOrManagedNote = "User grants Usage Access in Android Settings",
            consentFlow = "Panel is visible, excludes keyboard/passwords, and has an Off switch."
        ),
        FeatureCapabilitySpec(
            id = "installed_apps",
            title = "Installed Apps & Screen-Time Rules",
            status = ImplementationStatus.NEEDS_MANAGED_DEVICE,
            requiredPermission = "Usage Access + Managed Device / Family Link APIs",
            officialApi = "PackageManager + UsageStatsManager + DevicePolicyManager",
            roleOrManagedNote = "Universal app blocking requires Device Owner / Managed Profile",
            consentFlow = "Schedules and limits are visible to the child; uninstall uses system dialog."
        ),
        FeatureCapabilitySpec(
            id = "brightness_volume",
            title = "Brightness & Volume Controls",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "WRITE_SETTINGS (for system brightness)",
            officialApi = "AudioManager + Settings.System",
            roleOrManagedNote = "Volume works directly; system brightness needs Write Settings",
            consentFlow = "Shows clear permission prompt when Write Settings is not granted."
        ),
        FeatureCapabilitySpec(
            id = "device_utilities",
            title = "Speak, Show, Vibrate, Locate & Lock",
            status = ImplementationStatus.IMPLEMENTED,
            requiredPermission = "VIBRATE (Install-time); Lock needs DevicePolicyManager",
            officialApi = "TextToSpeech, Vibrator, RingtoneManager, DevicePolicyManager",
            roleOrManagedNote = "Remote Lock requires Device Admin / Managed Device enrollment",
            consentFlow = "Speak/Show/Vibrate/Locate are logged in Audit Logs and visible to child."
        ),
        FeatureCapabilitySpec(
            id = "notifications",
            title = "Notifications History",
            status = ImplementationStatus.NEEDS_PERMISSION,
            requiredPermission = "BIND_NOTIFICATION_LISTENER_SERVICE",
            officialApi = "NotificationListenerService",
            roleOrManagedNote = "Requires user grant in Notification Access Settings",
            consentFlow = "Respects sensitive notification redaction; no private chat scraping."
        ),
        FeatureCapabilitySpec(
            id = "call_logs",
            title = "Call Logs & Number Blocking",
            status = ImplementationStatus.NOT_SUPPORTED,
            requiredPermission = "READ_CALL_LOG / ROLE_DIALER (Restricted)",
            officialApi = "CallLog.Calls + TelecomManager + RoleManager",
            roleOrManagedNote = "Google Play restricts READ_CALL_LOG to default dialer apps",
            consentFlow = "Transparently explains Play policy restriction; no circumvention."
        ),
        FeatureCapabilitySpec(
            id = "sms_messages",
            title = "SMS Inbox",
            status = ImplementationStatus.NOT_SUPPORTED,
            requiredPermission = "READ_SMS / ROLE_SMS (Restricted)",
            officialApi = "Telephony.Sms + RoleManager",
            roleOrManagedNote = "Requires default SMS handler role; 3P chat apps never intercepted",
            consentFlow = "Never accesses WhatsApp, IMO, Messenger, or Gmail."
        ),
        FeatureCapabilitySpec(
            id = "contacts",
            title = "Contacts Picker",
            status = ImplementationStatus.IMPLEMENTED,
            requiredPermission = "Zero-permission PickContact / ContactsContract",
            officialApi = "ActivityResultContracts.PickContact + ContactsContract",
            roleOrManagedNote = "Uses Android system contact picker without broad READ_CONTACTS",
            consentFlow = "Changes require explicit user confirmation."
        )
    )
}
