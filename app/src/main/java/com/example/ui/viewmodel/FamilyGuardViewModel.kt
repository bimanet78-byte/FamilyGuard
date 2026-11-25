package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppMode
import com.example.data.model.AuditLog
import com.example.data.model.LocationShare
import com.example.data.model.ManagedDevice
import com.example.data.model.PairingCode
import com.example.data.model.ParentCommand
import com.example.data.model.ScreenTimeRule
import com.example.data.model.UserProfile
import com.example.data.repository.FamilyGuardRepository
import com.example.platform.AppUsageStatItem
import com.example.platform.DeviceTelemetryHelper
import com.example.platform.PermissionStatusItem
import com.example.platform.SafFileMetadata
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class FamilyGuardViewModel(
    private val repository: FamilyGuardRepository,
    val currentUserId: String,
    userDisplayName: String,
    userEmail: String
) : ViewModel() {

    private val _appMode = MutableStateFlow(AppMode.PARENT)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _usageStats = MutableStateFlow<List<AppUsageStatItem>>(emptyList())
    val usageStats: StateFlow<List<AppUsageStatItem>> = _usageStats.asStateFlow()

    private val _permissionStatuses = MutableStateFlow<List<PermissionStatusItem>>(emptyList())
    val permissionStatuses: StateFlow<List<PermissionStatusItem>> = _permissionStatuses.asStateFlow()

    private val _selectedSafFiles = MutableStateFlow<List<SafFileMetadata>>(emptyList())
    val selectedSafFiles: StateFlow<List<SafFileMetadata>> = _selectedSafFiles.asStateFlow()

    private val _selectedGalleryUris = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val selectedGalleryUris: StateFlow<List<android.net.Uri>> = _selectedGalleryUris.asStateFlow()

    private fun <T> Flow<T>.toUiStateFlow(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "$tag error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("Profile", "Unable to load user profile")

    val devicesState: StateFlow<UiState<List<ManagedDevice>>> =
        repository.observeDevices(currentUserId)
            .toUiStateFlow("Devices", "Unable to load connected devices")

    val pairingCodesState: StateFlow<UiState<List<PairingCode>>> =
        repository.observePairingCodes(currentUserId)
            .toUiStateFlow("PairingCodes", "Unable to load pairing codes")

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDevice: StateFlow<ManagedDevice?> =
        devicesState.map { state ->
            val list = (state as? UiState.Success)?.data.orEmpty()
            val targetId = _selectedDeviceId.value
            list.firstOrNull { it.id == targetId } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val commandsState: StateFlow<UiState<List<ParentCommand>>> =
        selectedDevice.flatMapLatest { device ->
            if (device == null) flowOf(emptyList())
            else repository.observeCommands(device.id, currentUserId)
        }.toUiStateFlow("Commands", "Unable to load commands")

    @OptIn(ExperimentalCoroutinesApi::class)
    val rulesState: StateFlow<UiState<List<ScreenTimeRule>>> =
        selectedDevice.flatMapLatest { device ->
            if (device == null) flowOf(emptyList())
            else repository.observeScreenTimeRules(device.id, currentUserId)
        }.toUiStateFlow("Rules", "Unable to load screen-time rules")

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogsState: StateFlow<UiState<List<AuditLog>>> =
        selectedDevice.flatMapLatest { device ->
            if (device == null) flowOf(emptyList())
            else repository.observeAuditLogs(device.id, currentUserId)
        }.toUiStateFlow("AuditLogs", "Unable to load audit logs")

    @OptIn(ExperimentalCoroutinesApi::class)
    val locationSharesState: StateFlow<UiState<List<LocationShare>>> =
        selectedDevice.flatMapLatest { device ->
            if (device == null) flowOf(emptyList())
            else repository.observeLocationShares(device.id, currentUserId)
        }.toUiStateFlow("LocationShares", "Unable to load location shares")

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(displayName = userDisplayName, email = userEmail)
        }
    }

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
        viewModelScope.launch {
            repository.switchUserRole(if (mode == AppMode.KID) "kid" else "parent")
        }
    }

    fun selectDevice(deviceId: String) {
        _selectedDeviceId.value = deviceId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showMessage(message: String) {
        _statusBanner.value = message
    }

    fun refreshLocalDeviceDiagnostics(context: Context) {
        _permissionStatuses.value = DeviceTelemetryHelper.getPermissionStatuses(context)
        _usageStats.value = DeviceTelemetryHelper.queryTodayUsageStats(context)
    }

    fun syncThisDeviceNow(context: Context, deviceId: String) {
        refreshLocalDeviceDiagnostics(context)
        val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
        viewModelScope.launch {
            val res = repository.syncTelemetryFromChild(deviceId, telemetry)
            _statusBanner.value = if (res.isSuccess) {
                "Synced telemetry (${telemetry.batteryPercent}%, ${telemetry.networkType})"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to sync telemetry"
            }
        }
    }

    fun linkCurrentDeviceForFamily(context: Context, kidName: String) {
        refreshLocalDeviceDiagnostics(context)
        val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
        viewModelScope.launch {
            val res = repository.linkThisDeviceWithTelemetry(
                parentUid = currentUserId,
                childUid = currentUserId,
                kidName = kidName,
                telemetry = telemetry
            )
            res.onSuccess { newId ->
                _selectedDeviceId.value = newId
                _statusBanner.value = "Linked ${telemetry.deviceName} as a supervised device"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to link device"
            }
        }
    }

    fun createPairingCode(parentName: String, deviceHint: String) {
        viewModelScope.launch {
            val res = repository.createPairingCode(parentName, deviceHint)
            res.onSuccess { code ->
                _statusBanner.value = "Pairing code $code generated (valid 15 min)"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not create pairing code"
            }
        }
    }

    fun revokePairingCode(code: String) {
        viewModelScope.launch {
            repository.revokePairingCode(code)
        }
    }

    fun claimPairingCode(context: Context, code: String, kidName: String) {
        refreshLocalDeviceDiagnostics(context)
        val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
        viewModelScope.launch {
            val res = repository.claimPairingCode(code, kidName, telemetry)
            res.onSuccess { deviceId ->
                _selectedDeviceId.value = deviceId
                _appMode.value = AppMode.KID
                _statusBanner.value = "Device paired with parent account!"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Pairing failed"
            }
        }
    }

    fun updateParentSettings(
        deviceId: String,
        kidName: String? = null,
        wallpaperId: String? = null,
        brightnessPercent: Int? = null,
        volumePercent: Int? = null,
        dailyLimitMinutes: Int? = null,
        newPin: String? = null
    ) {
        viewModelScope.launch {
            val res = repository.updateDeviceFromParent(
                deviceId = deviceId,
                kidName = kidName,
                wallpaperId = wallpaperId,
                brightnessPercent = brightnessPercent,
                volumePercent = volumePercent,
                dailyLimitMinutes = dailyLimitMinutes,
                newPin = newPin
            )
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Update failed"
            }
        }
    }

    fun sendParentCommand(device: ManagedDevice, commandType: String, payloadText: String) {
        viewModelScope.launch {
            val res = repository.sendCommand(device, commandType, payloadText)
            res.onSuccess {
                _statusBanner.value = "Sent $commandType request (awaiting child consent)"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Command failed"
            }
        }
    }

    fun executeOrRespondCommandOnChild(
        context: Context,
        device: ManagedDevice,
        command: ParentCommand,
        approve: Boolean
    ) {
        viewModelScope.launch {
            if (!approve) {
                repository.respondToCommand(device, command, "declined_by_child", "Declined by child on device")
                _statusBanner.value = "Declined ${command.commandType}"
                return@launch
            }
            val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
            when (command.commandType) {
                "SPEAK_MESSAGE" -> {
                    DeviceTelemetryHelper.speakMessageAloud(context, command.payloadText)
                    repository.respondToCommand(device, command, "executed", "Spoken aloud via Android TextToSpeech")
                }
                "SHOW_MESSAGE" -> {
                    repository.respondToCommand(device, command, "executed", "Displayed: ${command.payloadText}")
                }
                "VIBRATE" -> {
                    DeviceTelemetryHelper.triggerVibration(context)
                    repository.respondToCommand(device, command, "executed", "Device vibrated")
                }
                "LOCATE_SOUND" -> {
                    DeviceTelemetryHelper.playLocateRing(context)
                    repository.respondToCommand(device, command, "executed", "Played locate chime")
                }
                "SET_WALLPAPER" -> {
                    repository.syncTelemetryFromChild(device.id, telemetry, wallpaperId = command.payloadText)
                    repository.respondToCommand(device, command, "executed", "Applied approved wallpaper ${command.payloadText}")
                }
                "REQUEST_LOCATION" -> {
                    val locRes = DeviceTelemetryHelper.fetchCurrentLocation(context)
                    locRes.onSuccess { (lat, lng, acc) ->
                        repository.publishLocationShare(device, lat, lng, acc, "FusedLocation", true)
                        repository.syncTelemetryFromChild(device.id, telemetry, activeLocationSession = true)
                        repository.respondToCommand(device, command, "approved_by_child", "Location sharing active (±${acc.toInt()}m)")
                    }.onFailure { err ->
                        repository.respondToCommand(device, command, "declined_by_child", err.localizedMessage ?: "Location permission missing")
                    }
                }
                "REQUEST_CAMERA_FRONT", "REQUEST_CAMERA_BACK" -> {
                    repository.syncTelemetryFromChild(device.id, telemetry, activeCameraSession = true)
                    repository.respondToCommand(device, command, "approved_by_child", "Camera snapshot session approved by child")
                }
                "REQUEST_LIVE_AUDIO" -> {
                    repository.syncTelemetryFromChild(device.id, telemetry, activeAudioSession = true)
                    repository.respondToCommand(device, command, "approved_by_child", "Live audio session approved by child")
                }
                "REQUEST_SCREEN_SHARE" -> {
                    repository.syncTelemetryFromChild(device.id, telemetry, activeScreenShareSession = true)
                    repository.respondToCommand(device, command, "approved_by_child", "Screen sharing session active")
                }
                else -> {
                    repository.respondToCommand(device, command, "approved_by_child", "Confirmed by child device")
                }
            }
        }
    }

    fun toggleFloatingPanel(context: Context, device: ManagedDevice, enabled: Boolean) {
        val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
        viewModelScope.launch {
            repository.syncTelemetryFromChild(device.id, telemetry, floatingPanelEnabled = enabled)
            _statusBanner.value = if (enabled) "Floating usage panel visible" else "Floating usage panel turned off"
        }
    }

    fun stopActiveSession(context: Context, device: ManagedDevice, sessionType: String) {
        val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
        viewModelScope.launch {
            when (sessionType) {
                "location" -> {
                    repository.syncTelemetryFromChild(device.id, telemetry, activeLocationSession = false)
                    repository.publishLocationShare(device, 0.0, 0.0, 0.0, "Stopped", false)
                }
                "camera" -> repository.syncTelemetryFromChild(device.id, telemetry, activeCameraSession = false)
                "audio" -> repository.syncTelemetryFromChild(device.id, telemetry, activeAudioSession = false)
                "screen" -> repository.syncTelemetryFromChild(device.id, telemetry, activeScreenShareSession = false)
            }
            _statusBanner.value = "Stopped $sessionType session"
        }
    }

    fun startLocationShareFromChild(context: Context, device: ManagedDevice) {
        val telemetry = DeviceTelemetryHelper.collectTelemetry(context)
        viewModelScope.launch {
            val locRes = DeviceTelemetryHelper.fetchCurrentLocation(context)
            locRes.onSuccess { (lat, lng, acc) ->
                repository.publishLocationShare(device, lat, lng, acc, "FusedLocation", true)
                repository.syncTelemetryFromChild(device.id, telemetry, activeLocationSession = true)
                _statusBanner.value = "Sharing live location (±${acc.toInt()}m)"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Grant location permission first"
            }
        }
    }

    fun upsertScreenTimeRule(
        device: ManagedDevice,
        packageName: String,
        appName: String,
        dailyLimitMinutes: Int,
        paused: Boolean,
        scheduleLabel: String
    ) {
        viewModelScope.launch {
            val res = repository.upsertScreenTimeRule(device, packageName, appName, dailyLimitMinutes, paused, scheduleLabel)
            res.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not save rule"
            }
        }
    }

    fun unlinkDevice(deviceId: String) {
        viewModelScope.launch {
            val res = repository.unlinkDevice(deviceId)
            res.onSuccess {
                _selectedDeviceId.value = null
                _statusBanner.value = "Device unlinked cleanly"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Failed to unlink device"
            }
        }
    }

    fun addSafFile(context: Context, uri: android.net.Uri) {
        val meta = DeviceTelemetryHelper.readSafMetadata(context, uri)
        _selectedSafFiles.value = (_selectedSafFiles.value + meta).distinctBy { it.uri }
    }

    fun removeSafFile(uri: android.net.Uri) {
        _selectedSafFiles.value = _selectedSafFiles.value.filterNot { it.uri == uri }
    }

    fun addGalleryUris(uris: List<android.net.Uri>) {
        _selectedGalleryUris.value = (_selectedGalleryUris.value + uris).distinct()
    }

    fun removeGalleryUri(uri: android.net.Uri) {
        _selectedGalleryUris.value = _selectedGalleryUris.value.filterNot { it == uri }
    }

    private companion object {
        const val TAG = "FamilyGuardVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
