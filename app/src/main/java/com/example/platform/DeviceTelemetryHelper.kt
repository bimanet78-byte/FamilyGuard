package com.example.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceTelemetrySnapshot(
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val batteryPercent: Int,
    val batteryCharging: Boolean,
    val batteryTempC: Int,
    val batteryCurrentMa: Int,
    val networkType: String,
    val activeAppName: String,
    val brightnessPercent: Int,
    val volumePercent: Int,
    val ringerMode: String,
    val dailyScreenTimeMinutes: Int,
    val grantedPermissionsCount: Int,
    val totalPermissionsCount: Int
)

data class AppUsageStatItem(
    val packageName: String,
    val appName: String,
    val totalMinutesToday: Int,
    val lastUsedMillis: Long
)

data class PermissionStatusItem(
    val key: String,
    val title: String,
    val description: String,
    val granted: Boolean,
    val kind: PermissionKind
)

enum class PermissionKind {
    RUNTIME_CAMERA,
    RUNTIME_AUDIO,
    RUNTIME_LOCATION,
    RUNTIME_NOTIFICATIONS,
    SPECIAL_USAGE_ACCESS,
    SPECIAL_WRITE_SETTINGS,
    SPECIAL_NOTIFICATION_LISTENER,
    MANAGED_DEVICE_ADMIN
}

data class SafFileMetadata(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)

object DeviceTelemetryHelper {

    fun collectTelemetry(context: Context): DeviceTelemetrySnapshot {
        val manufacturer = Build.MANUFACTURER.uppercase(Locale.US)
        val model = Build.MODEL.uppercase(Locale.US)
        val deviceName = if (model.startsWith(manufacturer)) model else "$manufacturer $model"
        val androidVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100) / scale).coerceIn(0, 100) else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempTenths / 10

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = currentUa / 1000

        val networkType = detectNetworkType(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        val volumePercent = ((curVol * 100) / maxVol).coerceIn(0, 100)
        val ringerMode = when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "silent"
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
            else -> "ring"
        }

        val brightnessRaw = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Exception) {
            160
        }
        val brightnessPercent = ((brightnessRaw * 100) / 255).coerceIn(0, 100)

        val usageList = queryTodayUsageStats(context)
        val activeAppName = usageList.firstOrNull()?.appName ?: "FamilyGuard"
        val totalMinutes = usageList.sumOf { it.totalMinutesToday }.coerceIn(0, 1440)

        val perms = getPermissionStatuses(context)
        return DeviceTelemetrySnapshot(
            deviceName = deviceName,
            manufacturer = manufacturer,
            model = model,
            androidVersion = androidVersion,
            batteryPercent = batteryPct,
            batteryCharging = charging,
            batteryTempC = tempC,
            batteryCurrentMa = currentMa,
            networkType = networkType,
            activeAppName = activeAppName,
            brightnessPercent = brightnessPercent,
            volumePercent = volumePercent,
            ringerMode = ringerMode,
            dailyScreenTimeMinutes = totalMinutes,
            grantedPermissionsCount = perms.count { it.granted },
            totalPermissionsCount = perms.size
        )
    }

    private fun detectNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Offline"
        val net = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(net) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasNotificationListenerAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(context.packageName)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return dpm.activeAdmins?.any { it.packageName == context.packageName } == true
    }

    fun getPermissionStatuses(context: Context): List<PermissionStatusItem> {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val usageGranted = hasUsageStatsPermission(context)
        val writeSettingsGranted = Settings.System.canWrite(context)
        val notifListenerGranted = hasNotificationListenerAccess(context)
        val adminGranted = isDeviceAdminActive(context)

        return listOf(
            PermissionStatusItem(
                key = "camera",
                title = "Camera (Consent-Gated)",
                description = "Allows child-approved Front/Back snapshots when requested.",
                granted = cameraGranted,
                kind = PermissionKind.RUNTIME_CAMERA
            ),
            PermissionStatusItem(
                key = "audio",
                title = "Microphone (Live Audio Session)",
                description = "Allows child-approved live audio with visible timer & stop.",
                granted = audioGranted,
                kind = PermissionKind.RUNTIME_AUDIO
            ),
            PermissionStatusItem(
                key = "location",
                title = "Location Sharing",
                description = "Shares device coordinates only while Location Sharing is active.",
                granted = locationGranted,
                kind = PermissionKind.RUNTIME_LOCATION
            ),
            PermissionStatusItem(
                key = "notifications",
                title = "Safety Alerts & Active Session Badge",
                description = "Shows persistent notifications whenever a session is running.",
                granted = notifGranted,
                kind = PermissionKind.RUNTIME_NOTIFICATIONS
            ),
            PermissionStatusItem(
                key = "usage_access",
                title = "Usage Access (UsageStatsManager)",
                description = "Powers real daily screen-time charts and the Floating Usage Panel.",
                granted = usageGranted,
                kind = PermissionKind.SPECIAL_USAGE_ACCESS
            ),
            PermissionStatusItem(
                key = "write_settings",
                title = "Modify System Brightness",
                description = "Allows adjusting display brightness when permitted by Android.",
                granted = writeSettingsGranted,
                kind = PermissionKind.SPECIAL_WRITE_SETTINGS
            ),
            PermissionStatusItem(
                key = "notif_listener",
                title = "Notification Access",
                description = "Reads non-sensitive notification summaries when explicitly enabled.",
                granted = notifListenerGranted,
                kind = PermissionKind.SPECIAL_NOTIFICATION_LISTENER
            ),
            PermissionStatusItem(
                key = "device_admin",
                title = "Managed Device / DevicePolicyManager",
                description = "Required by Android for remote screen lock and universal app pause.",
                granted = adminGranted,
                kind = PermissionKind.MANAGED_DEVICE_ADMIN
            )
        )
    }

    fun queryTodayUsageStats(context: Context): List<AppUsageStatItem> {
        if (!hasUsageStatsPermission(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val end = System.currentTimeMillis()
        val start = end - 24L * 60L * 60L * 1000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
        val pm = context.packageManager
        return stats
            .filter { it.totalTimeInForeground > 10_000L }
            .groupBy { it.packageName }
            .map { (pkg, entries) ->
                val totalMs = entries.sumOf { it.totalTimeInForeground }
                val lastUsed = entries.maxOfOrNull { it.lastTimeUsed } ?: 0L
                val appLabel = try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (_: Exception) {
                    pkg.substringAfterLast('.')
                }
                AppUsageStatItem(
                    packageName = pkg,
                    appName = appLabel,
                    totalMinutesToday = (totalMs / 60_000L).toInt().coerceAtLeast(1),
                    lastUsedMillis = lastUsed
                )
            }
            .sortedByDescending { it.lastUsedMillis }
            .take(25)
    }

    fun setMediaVolumePercent(context: Context, percent: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((percent.coerceIn(0, 100) * maxVol) / 100.0).toInt().coerceIn(0, maxVol)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

    fun setSystemBrightnessPercent(context: Context, percent: Int): Boolean {
        if (!Settings.System.canWrite(context)) return false
        val value = ((percent.coerceIn(5, 100) * 255) / 100.0).toInt().coerceIn(10, 255)
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun triggerVibration(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(450)
            }
        }
    }

    fun playLocateRing(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (_: Exception) {
        }
    }

    fun speakMessageAloud(context: Context, text: String) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fg_msg_${System.currentTimeMillis()}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<Triple<Double, Double, Double>> {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(Result.success(Triple(loc.latitude, loc.longitude, loc.accuracy.toDouble())))
                    } else {
                        client.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                cont.resume(Result.success(Triple(last.latitude, last.longitude, last.accuracy.toDouble())))
                            } else {
                                cont.resume(Result.failure(IllegalStateException("Location unavailable; enable GPS/location services")))
                            }
                        }.addOnFailureListener { e -> cont.resume(Result.failure(e)) }
                    }
                }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            cont.invokeOnCancellation { cts.cancel() }
        }
    }

    fun readSafMetadata(context: Context, uri: Uri): SafFileMetadata {
        var name = uri.lastPathSegment ?: "Selected file"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        return SafFileMetadata(uri = uri, displayName = name, mimeType = mime, sizeBytes = size)
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openWriteSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
