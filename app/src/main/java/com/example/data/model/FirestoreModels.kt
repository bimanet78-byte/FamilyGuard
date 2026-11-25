package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "parent",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PairingCode(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val parentName: String = "",
    val deviceNameHint: String = "",
    val status: String = "pending",
    val claimedByUid: String = "",
    val claimedDeviceId: String = "",
    val ttlMinutes: Long = 15L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ManagedDevice(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val childUid: String = "",
    val memberUids: List<String> = emptyList(),
    val deviceName: String = "",
    val kidName: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val androidVersion: String = "",
    val kidsAppVersion: String = "1.8.0",
    val connectionState: String = "online",
    val batteryPercent: Long = 0L,
    val batteryCharging: Boolean = false,
    val batteryTempC: Long = 0L,
    val batteryCurrentMa: Long = 0L,
    val networkType: String = "-",
    val activeAppName: String = "FamilyGuard",
    val wallpaperId: String = "starstreak",
    val brightnessPercent: Long = 70L,
    val volumePercent: Long = 60L,
    val ringerMode: String = "ring",
    val dailyScreenTimeMinutes: Long = 0L,
    val dailyLimitMinutes: Long = 180L,
    val pinHash: String = "",
    val floatingPanelEnabled: Boolean = true,
    val activeLocationSession: Boolean = false,
    val activeCameraSession: Boolean = false,
    val activeAudioSession: Boolean = false,
    val activeScreenShareSession: Boolean = false,
    val permissionsGrantedCount: Long = 0L,
    val permissionsTotalCount: Long = 8L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "parentUid" to parentUid,
        "childUid" to childUid,
        "memberUids" to memberUids,
        "deviceName" to deviceName.take(60),
        "kidName" to kidName.take(40),
        "manufacturer" to manufacturer.take(40),
        "model" to model.take(40),
        "androidVersion" to androidVersion.take(20),
        "kidsAppVersion" to kidsAppVersion.take(20),
        "connectionState" to connectionState,
        "batteryPercent" to batteryPercent.coerceIn(0L, 100L),
        "batteryCharging" to batteryCharging,
        "batteryTempC" to batteryTempC.coerceIn(-30L, 90L),
        "batteryCurrentMa" to batteryCurrentMa.coerceIn(-15000L, 15000L),
        "networkType" to networkType.take(30),
        "activeAppName" to activeAppName.take(80),
        "wallpaperId" to wallpaperId.take(40),
        "brightnessPercent" to brightnessPercent.coerceIn(0L, 100L),
        "volumePercent" to volumePercent.coerceIn(0L, 100L),
        "ringerMode" to ringerMode,
        "dailyScreenTimeMinutes" to dailyScreenTimeMinutes.coerceIn(0L, 1440L),
        "dailyLimitMinutes" to dailyLimitMinutes.coerceIn(15L, 1440L),
        "pinHash" to pinHash.take(128),
        "floatingPanelEnabled" to floatingPanelEnabled,
        "activeLocationSession" to activeLocationSession,
        "activeCameraSession" to activeCameraSession,
        "activeAudioSession" to activeAudioSession,
        "activeScreenShareSession" to activeScreenShareSession,
        "permissionsGrantedCount" to permissionsGrantedCount.coerceIn(0L, 30L),
        "permissionsTotalCount" to permissionsTotalCount.coerceIn(1L, 30L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ParentCommand(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val childUid: String = "",
    val memberUids: List<String> = emptyList(),
    val commandType: String = "",
    val payloadText: String = "",
    val status: String = "pending",
    val resultMessage: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScreenTimeRule(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val childUid: String = "",
    val memberUids: List<String> = emptyList(),
    val packageName: String = "",
    val appName: String = "",
    val dailyLimitMinutes: Long = 60L,
    val paused: Boolean = false,
    val scheduleLabel: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val childUid: String = "",
    val memberUids: List<String> = emptyList(),
    val actorUid: String = "",
    val actorRole: String = "system",
    val action: String = "",
    val details: String = "",
    val status: String = "ok",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LocationShare(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val childUid: String = "",
    val memberUids: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Double = 0.0,
    val provider: String = "",
    val active: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

enum class AppMode { PARENT, KID }
