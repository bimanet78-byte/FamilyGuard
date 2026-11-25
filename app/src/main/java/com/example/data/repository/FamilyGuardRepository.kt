package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditLog
import com.example.data.model.LocationShare
import com.example.data.model.ManagedDevice
import com.example.data.model.PairingCode
import com.example.data.model.ParentCommand
import com.example.data.model.ScreenTimeRule
import com.example.data.model.UserProfile
import com.example.platform.DeviceTelemetrySnapshot
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import kotlin.random.Random

class FamilyGuardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.wrapFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val authedUid = requireUserId()
        check(authedUid == uid) { "Auth UID mismatch." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeDevices(uid: String): Flow<List<ManagedDevice>> = flow {
        val authedUid = requireUserId()
        check(authedUid == uid) { "Auth UID mismatch." }
        val path = "devices"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ManagedDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePairingCodes(parentUid: String): Flow<List<PairingCode>> = flow {
        val authedUid = requireUserId()
        check(authedUid == parentUid) { "Auth UID mismatch." }
        val path = "pairing_codes"
        emitAll(
            db.collection(path)
                .whereEqualTo("parentUid", parentUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PairingCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, parentUid)
        )
    }

    fun observeCommands(deviceId: String, uid: String): Flow<List<ParentCommand>> = flow {
        val authedUid = requireUserId()
        check(authedUid == uid) { "Auth UID mismatch." }
        val path = "devices/$deviceId/commands"
        emitAll(
            db.collection("devices").document(deviceId).collection("commands")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ParentCommand::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeScreenTimeRules(deviceId: String, uid: String): Flow<List<ScreenTimeRule>> = flow {
        val authedUid = requireUserId()
        check(authedUid == uid) { "Auth UID mismatch." }
        val path = "devices/$deviceId/screen_time_rules"
        emitAll(
            db.collection("devices").document(deviceId).collection("screen_time_rules")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ScreenTimeRule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.appName.lowercase() }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAuditLogs(deviceId: String, uid: String): Flow<List<AuditLog>> = flow {
        val authedUid = requireUserId()
        check(authedUid == uid) { "Auth UID mismatch." }
        val path = "devices/$deviceId/audit_logs"
        emitAll(
            db.collection("devices").document(deviceId).collection("audit_logs")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeLocationShares(deviceId: String, uid: String): Flow<List<LocationShare>> = flow {
        val authedUid = requireUserId()
        check(authedUid == uid) { "Auth UID mismatch." }
        val path = "devices/$deviceId/location_shares"
        emitAll(
            db.collection("devices").document(deviceId).collection("location_shares")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LocationShare::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun ensureUserProfile(displayName: String, email: String, defaultRole: String = "parent"): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val cleanName = displayName.trim().ifBlank { "Family Member" }.take(80)
            val cleanEmail = email.trim().take(120)
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "role" to if (defaultRole == "kid") "kid" else "parent",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun switchUserRole(role: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val safeRole = if (role == "kid") "kid" else "parent"
            db.collection("users").document(uid)
                .update(mapOf("role" to safeRole, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPairingCode(parentName: String, deviceNameHint: String): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = buildString {
            repeat(6) { append(chars[Random.nextInt(chars.length)]) }
        }
        val path = "pairing_codes/$code"
        return try {
            val payload = mapOf(
                "parentUid" to uid,
                "parentName" to parentName.trim().ifBlank { "Parent" }.take(80),
                "deviceNameHint" to deviceNameHint.trim().take(80),
                "status" to "pending",
                "ttlMinutes" to 15L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("pairing_codes").document(code).set(payload).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun revokePairingCode(code: String): Result<Unit> {
        val path = "pairing_codes/$code"
        return try {
            requireUserId()
            db.collection("pairing_codes").document(code)
                .update(mapOf("status" to "revoked", "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun linkThisDeviceWithTelemetry(
        parentUid: String,
        childUid: String,
        kidName: String,
        telemetry: DeviceTelemetrySnapshot
    ): Result<String> {
        val path = "devices"
        return try {
            requireUserId()
            val docRef = db.collection("devices").document()
            val members = listOf(parentUid, childUid).distinct()
            val device = ManagedDevice(
                parentUid = parentUid,
                childUid = childUid,
                memberUids = members,
                deviceName = telemetry.deviceName,
                kidName = kidName.trim().ifBlank { telemetry.deviceName }.take(40),
                manufacturer = telemetry.manufacturer,
                model = telemetry.model,
                androidVersion = telemetry.androidVersion,
                kidsAppVersion = "1.8.0",
                connectionState = "online",
                batteryPercent = telemetry.batteryPercent.toLong(),
                batteryCharging = telemetry.batteryCharging,
                batteryTempC = telemetry.batteryTempC.toLong(),
                batteryCurrentMa = telemetry.batteryCurrentMa.toLong(),
                networkType = telemetry.networkType,
                activeAppName = telemetry.activeAppName,
                wallpaperId = "starstreak",
                brightnessPercent = telemetry.brightnessPercent.toLong(),
                volumePercent = telemetry.volumePercent.toLong(),
                ringerMode = telemetry.ringerMode,
                dailyScreenTimeMinutes = telemetry.dailyScreenTimeMinutes.toLong(),
                dailyLimitMinutes = 180L,
                pinHash = "",
                floatingPanelEnabled = true,
                activeLocationSession = false,
                activeCameraSession = false,
                activeAudioSession = false,
                activeScreenShareSession = false,
                permissionsGrantedCount = telemetry.grantedPermissionsCount.toLong(),
                permissionsTotalCount = telemetry.totalPermissionsCount.toLong()
            )
            docRef.set(device.toCreateMap()).await()
            addAuditLogInternal(
                deviceId = docRef.id,
                parentUid = parentUid,
                childUid = childUid,
                memberUids = members,
                actorRole = "child",
                action = "DEVICE_PAIRED",
                details = "Linked ${device.deviceName} with transparent consent controls",
                status = "ok"
            )
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimPairingCode(
        rawCode: String,
        kidName: String,
        telemetry: DeviceTelemetrySnapshot
    ): Result<String> {
        val childUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val code = rawCode.trim().uppercase()
        if (code.length != 6 || !code.matches(Regex("^[A-Z0-9]+$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 6-character pairing code"))
        }
        val path = "pairing_codes/$code"
        return try {
            val codeSnap = db.collection("pairing_codes").document(code).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Pairing code not found"))
            }
            val pairing = codeSnap.toObject(PairingCode::class.java)
                ?: return Result.failure(IllegalStateException("Invalid pairing code record"))
            if (pairing.status != "pending") {
                return Result.failure(IllegalStateException("Pairing code is already ${pairing.status}"))
            }

            val linked = linkThisDeviceWithTelemetry(
                parentUid = pairing.parentUid,
                childUid = childUid,
                kidName = kidName,
                telemetry = telemetry
            )
            val deviceId = linked.getOrThrow()

            db.collection("pairing_codes").document(code).update(
                mapOf(
                    "status" to "claimed",
                    "claimedByUid" to childUid,
                    "claimedDeviceId" to deviceId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(deviceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDeviceFromParent(
        deviceId: String,
        kidName: String? = null,
        wallpaperId: String? = null,
        brightnessPercent: Int? = null,
        volumePercent: Int? = null,
        dailyLimitMinutes: Int? = null,
        newPin: String? = null,
        connectionState: String? = null
    ): Result<Unit> {
        val path = "devices/$deviceId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            kidName?.let { updates["kidName"] = it.trim().ifBlank { "Kid Device" }.take(40) }
            wallpaperId?.let { updates["wallpaperId"] = it.take(40) }
            brightnessPercent?.let { updates["brightnessPercent"] = it.coerceIn(0, 100).toLong() }
            volumePercent?.let { updates["volumePercent"] = it.coerceIn(0, 100).toLong() }
            dailyLimitMinutes?.let { updates["dailyLimitMinutes"] = it.coerceIn(15, 1440).toLong() }
            newPin?.let { updates["pinHash"] = sha256(it) }
            connectionState?.let { updates["connectionState"] = it }
            db.collection("devices").document(deviceId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncTelemetryFromChild(
        deviceId: String,
        telemetry: DeviceTelemetrySnapshot,
        floatingPanelEnabled: Boolean? = null,
        activeLocationSession: Boolean? = null,
        activeCameraSession: Boolean? = null,
        activeAudioSession: Boolean? = null,
        activeScreenShareSession: Boolean? = null,
        wallpaperId: String? = null
    ): Result<Unit> {
        val path = "devices/$deviceId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "deviceName" to telemetry.deviceName.take(60),
                "androidVersion" to telemetry.androidVersion.take(20),
                "connectionState" to "online",
                "batteryPercent" to telemetry.batteryPercent.coerceIn(0, 100).toLong(),
                "batteryCharging" to telemetry.batteryCharging,
                "batteryTempC" to telemetry.batteryTempC.coerceIn(-30, 90).toLong(),
                "batteryCurrentMa" to telemetry.batteryCurrentMa.coerceIn(-15000, 15000).toLong(),
                "networkType" to telemetry.networkType.take(30),
                "activeAppName" to telemetry.activeAppName.take(80),
                "brightnessPercent" to telemetry.brightnessPercent.coerceIn(0, 100).toLong(),
                "volumePercent" to telemetry.volumePercent.coerceIn(0, 100).toLong(),
                "ringerMode" to telemetry.ringerMode,
                "dailyScreenTimeMinutes" to telemetry.dailyScreenTimeMinutes.coerceIn(0, 1440).toLong(),
                "permissionsGrantedCount" to telemetry.grantedPermissionsCount.toLong(),
                "permissionsTotalCount" to telemetry.totalPermissionsCount.toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            floatingPanelEnabled?.let { updates["floatingPanelEnabled"] = it }
            activeLocationSession?.let { updates["activeLocationSession"] = it }
            activeCameraSession?.let { updates["activeCameraSession"] = it }
            activeAudioSession?.let { updates["activeAudioSession"] = it }
            activeScreenShareSession?.let { updates["activeScreenShareSession"] = it }
            wallpaperId?.let { updates["wallpaperId"] = it.take(40) }
            db.collection("devices").document(deviceId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendCommand(
        device: ManagedDevice,
        commandType: String,
        payloadText: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "devices/${device.id}/commands"
        return try {
            val cmdRef = db.collection("devices").document(device.id).collection("commands").document()
            val payload = mapOf(
                "parentUid" to device.parentUid,
                "childUid" to device.childUid,
                "memberUids" to device.memberUids,
                "commandType" to commandType.take(40),
                "payloadText" to payloadText.take(500),
                "status" to "pending",
                "resultMessage" to "Awaiting child confirmation",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            cmdRef.set(payload).await()
            addAuditLogInternal(
                deviceId = device.id,
                parentUid = device.parentUid,
                childUid = device.childUid,
                memberUids = device.memberUids,
                actorRole = if (uid == device.parentUid) "parent" else "child",
                action = commandType,
                details = payloadText.ifBlank { "Parent requested $commandType" }.take(300),
                status = "pending_consent"
            )
            Result.success(cmdRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToCommand(
        device: ManagedDevice,
        command: ParentCommand,
        newStatus: String,
        resultMessage: String
    ): Result<Unit> {
        val path = "devices/${device.id}/commands/${command.id}"
        return try {
            val uid = requireUserId()
            db.collection("devices").document(device.id)
                .collection("commands").document(command.id)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "resultMessage" to resultMessage.take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            addAuditLogInternal(
                deviceId = device.id,
                parentUid = device.parentUid,
                childUid = device.childUid,
                memberUids = device.memberUids,
                actorRole = if (uid == device.childUid) "child" else "parent",
                action = command.commandType,
                details = resultMessage.take(300),
                status = when (newStatus) {
                    "declined_by_child" -> "declined"
                    "stopped" -> "revoked"
                    else -> "ok"
                }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertScreenTimeRule(
        device: ManagedDevice,
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int,
        paused: Boolean,
        scheduleLabel: String
    ): Result<Unit> {
        val docId = packageName.replace(".", "_")
        val path = "devices/${device.id}/screen_time_rules/$docId"
        return try {
            requireUserId()
            val ref = db.collection("devices").document(device.id)
                .collection("screen_time_rules").document(docId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "parentUid" to device.parentUid,
                        "childUid" to device.childUid,
                        "memberUids" to device.memberUids,
                        "packageName" to packageName.take(120),
                        "appName" to appName.take(60),
                        "dailyLimitMinutes" to dailyLimitMinutes.coerceIn(5, 1440).toLong(),
                        "paused" to paused,
                        "scheduleLabel" to scheduleLabel.take(60),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "appName" to appName.take(60),
                        "dailyLimitMinutes" to dailyLimitMinutes.coerceIn(5, 1440).toLong(),
                        "paused" to paused,
                        "scheduleLabel" to scheduleLabel.take(60),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun publishLocationShare(
        device: ManagedDevice,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        provider: String,
        active: Boolean
    ): Result<Unit> {
        val path = "devices/${device.id}/location_shares/current"
        return try {
            requireUserId()
            val ref = db.collection("devices").document(device.id)
                .collection("location_shares").document("current")
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "parentUid" to device.parentUid,
                        "childUid" to device.childUid,
                        "memberUids" to device.memberUids,
                        "latitude" to latitude.coerceIn(-90.0, 90.0),
                        "longitude" to longitude.coerceIn(-180.0, 180.0),
                        "accuracyMeters" to accuracyMeters.coerceIn(0.0, 10000.0),
                        "provider" to provider.take(40),
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "latitude" to latitude.coerceIn(-90.0, 90.0),
                        "longitude" to longitude.coerceIn(-180.0, 180.0),
                        "accuracyMeters" to accuracyMeters.coerceIn(0.0, 10000.0),
                        "provider" to provider.take(40),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun unlinkDevice(deviceId: String): Result<Unit> {
        val path = "devices/$deviceId"
        return try {
            requireUserId()
            db.collection("devices").document(deviceId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun addAuditLogInternal(
        deviceId: String,
        parentUid: String,
        childUid: String,
        memberUids: List<String>,
        actorRole: String,
        action: String,
        details: String,
        status: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val logRef = db.collection("devices").document(deviceId).collection("audit_logs").document()
        logRef.set(
            mapOf(
                "parentUid" to parentUid,
                "childUid" to childUid,
                "memberUids" to memberUids,
                "actorUid" to uid,
                "actorRole" to actorRole,
                "action" to action.take(80),
                "details" to details.take(300),
                "status" to status,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
