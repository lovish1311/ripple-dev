package app.ripple.mesh.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ripple.mesh.audio.OpusPlayer
import app.ripple.mesh.audio.OpusRecorder
import app.ripple.mesh.core.Backup
import app.ripple.mesh.core.BatteryProfile
import app.ripple.mesh.core.NodeId
import app.ripple.mesh.core.toHex
import app.ripple.mesh.data.ConversationSummary
import app.ripple.mesh.data.IdentityStore
import app.ripple.mesh.data.MessageEntity
import app.ripple.mesh.data.MessageStatus
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.data.RippleDatabase
import app.ripple.mesh.data.SosBeaconEntity
import app.ripple.mesh.data.repository.MeshRepository
import app.ripple.mesh.service.LinkInfo
import app.ripple.mesh.service.MeshService
import app.ripple.mesh.service.MeshStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeshViewModel @Inject constructor(
    private val repository: MeshRepository,
    app: Application,
) : AndroidViewModel(app) {
    private val service = MutableStateFlow<MeshService?>(null)

    val status: StateFlow<MeshStatus> = service.flatMapLatest { it?.status ?: flowOf(MeshStatus()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeshStatus())

    val powerProfile: StateFlow<Int> = service.flatMapLatest { it?.powerProfile ?: flowOf(BatteryProfile.BALANCED) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatteryProfile.BALANCED)

    val selfId: StateFlow<NodeId?> = service.flatMapLatest { flowOf(it?.router?.selfId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Hex of our own 65-byte P-256 public key wire form, once the service is bound.
     * Input for pairing codes/safety numbers (docs/PAIRING.md) — never log it.
     */
    val selfPublicKeyHex: StateFlow<String?> = service.flatMapLatest { flowOf(it?.router?.identity?.publicKeyWire?.toHex()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val displayName: StateFlow<String?> = IdentityStore.displayName(app)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userAvatar: StateFlow<String> = IdentityStore.avatar(app)
        .map { it ?: "🧑‍🚀" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "🧑‍🚀")

    fun setUserAvatar(avatar: String) = viewModelScope.launch {
        IdentityStore.setAvatar(getApplication(), avatar)
        service.value?.setUserAvatar(avatar)
    }

    val themeMode: StateFlow<String> = IdentityStore.themeMode(app)
        .map { it ?: "system" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setThemeMode(mode: String) = viewModelScope.launch {
        IdentityStore.setThemeMode(getApplication(), mode)
    }

    val fontFamily: StateFlow<String> = IdentityStore.fontFamily(app)
        .map { it ?: "system" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setFontFamily(family: String) = viewModelScope.launch {
        IdentityStore.setFontFamily(getApplication(), family)
    }

    val fontScale: StateFlow<Float> = IdentityStore.fontScale(app)
        .map { it ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun setFontScale(scale: Float) = viewModelScope.launch {
        IdentityStore.setFontScale(getApplication(), scale)
    }

    val conversations: StateFlow<List<ConversationSummary>> = repository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val peers: StateFlow<List<PeerEntity>> = repository.observePeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Active unacknowledged SOS beacons (drives the top home alert banner). */
    val activeSosBeacons: StateFlow<List<SosBeaconEntity>> = repository.observeActiveSosBeacons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Received SOS beacon history (persisted in Room, pruned to ~90 days). */
    val allSosBeacons: StateFlow<List<SosBeaconEntity>> = repository.observeSosBeacons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sosBeacons: StateFlow<List<SosBeaconEntity>> get() = allSosBeacons

    fun acknowledgeSosBeacon(messageId: String) = viewModelScope.launch {
        repository.setSosBeaconAcknowledged(messageId, true)
    }

    fun acknowledgeAllSosBeacons() = viewModelScope.launch {
        repository.acknowledgeAllSosBeacons()
    }

    fun dismissSosBeacon(messageId: String) = viewModelScope.launch {
        repository.setSosBeaconAcknowledged(messageId, true)
    }

    fun deleteSosBeacon(messageId: String) = viewModelScope.launch {
        repository.deleteSosBeacon(messageId)
    }

    fun messages(conversation: String): Flow<List<MessageEntity>> = repository.observeMessages(conversation)
    fun peer(nodeIdHex: String): Flow<PeerEntity?> = repository.observePeer(nodeIdHex)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) { service.value = (binder as MeshService.LocalBinder).service }
        override fun onServiceDisconnected(name: ComponentName) { service.value = null }
    }

    fun bind() {
        val ctx = getApplication<Application>()
        MeshService.start(ctx)
        ctx.bindService(Intent(ctx, MeshService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    fun setVisibleConversation(c: String?) {
        service.value?.visibleConversation = c
        if (c != null) viewModelScope.launch { repository.markRead(c) }
    }

    fun send(conversation: String, text: String, replyTo: MessageEntity? = null) = viewModelScope.launch {
        val s = service.value ?: return@launch
        val replyId = replyTo?.messageId
        val replyText = replyTo?.text
        val replySender = replyTo?.let { it.fromName ?: NodeId.fromHex(it.fromNodeId).display }
        if (conversation == MeshService.BROADCAST_CONVERSATION) {
            s.sendBroadcast(text, replyToMessageId = replyId, replyToText = replyText, replyToSender = replySender)
        } else {
            s.sendDirect(NodeId.fromHex(conversation), text, replyToMessageId = replyId, replyToText = replyText, replyToSender = replySender)
        }
    }

    fun deleteMessageForMe(messageId: String) = viewModelScope.launch {
        repository.deleteMessage(messageId)
    }

    fun restoreMessage(message: MessageEntity) = viewModelScope.launch {
        repository.saveMessage(message)
    }

    fun deleteMessageForEveryone(messageId: String) = viewModelScope.launch {
        repository.markDeletedForEveryone(messageId)
    }

    fun editMessage(messageId: String, newText: String) = viewModelScope.launch {
        repository.editMessage(messageId, newText)
    }

    fun forwardMessage(targetConversation: String, text: String) = viewModelScope.launch {
        val s = service.value ?: return@launch
        if (targetConversation == MeshService.BROADCAST_CONVERSATION) {
            s.sendBroadcast(text, isForwarded = true)
        } else {
            s.sendDirect(NodeId.fromHex(targetConversation), text, isForwarded = true)
        }
    }

    val opusRecorder = OpusRecorder(getApplication())
    val opusPlayer = OpusPlayer(getApplication())

    val isRecording: StateFlow<Boolean> = opusRecorder.isRecording
    val recordingAmplitude: StateFlow<Float> = opusRecorder.amplitude
    val recordingDurationMs: StateFlow<Long> = opusRecorder.recordingDurationMs
    val playbackState: StateFlow<OpusPlayer.PlaybackState> = opusPlayer.playbackState

    fun startRecording(onAutoStop: ((OpusRecorder.RecordingResult) -> Unit)? = null) =
        opusRecorder.startRecording(viewModelScope, onAutoStop)

    suspend fun stopRecording(): OpusRecorder.RecordingResult? = opusRecorder.stopRecording()

    fun cancelRecording() = opusRecorder.cancelRecording()

    fun playVoice(messageId: String, filePath: String) = opusPlayer.play(viewModelScope, messageId, filePath)

    fun pauseVoice() = opusPlayer.pause()

    fun stopVoice() = opusPlayer.stop()

    fun sendVoiceMessage(conversation: String, file: java.io.File, durationMs: Int) = viewModelScope.launch {
        service.value?.sendVoice(conversation, file, durationMs)
    }

    fun setDisplayName(name: String) = viewModelScope.launch { service.value?.setDisplayName(name) ?: IdentityStore.setDisplayName(getApplication(), name) }

    fun setPowerProfile(code: Int) = viewModelScope.launch { service.value?.setPowerProfile(code) }

    fun sendSos(
        text: String,
        shareLocation: Boolean,
        voiceFile: java.io.File? = null,
        voiceDurationMs: Int? = null
    ) = viewModelScope.launch { service.value?.sendSos(text, shareLocation, voiceFile, voiceDurationMs) }

    /**
     * Simulates an incoming emergency SOS broadcast from a peer (for diagnostics, field testing, and UI verification).
     */
    fun simulateIncomingSos(
        fromName: String = "Asha",
        fromNodeId: String = "1e61a2b3c4d5e6f7",
        text: String = "Need immediate medical help, severe ankle sprain near North Trail marker 4.",
        lat: Double = 37.7749,
        lng: Double = -122.4194
    ) = viewModelScope.launch {
        val msgId = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
        val entity = SosBeaconEntity(
            messageId = msgId,
            fromNodeId = fromNodeId,
            fromName = fromName,
            text = text,
            latE7 = (lat * 1e7).toInt(),
            lngE7 = (lng * 1e7).toInt(),
            accuracyMeters = 15,
            verified = true,
            timestamp = System.currentTimeMillis()
        )
        repository.saveSosBeacon(entity)
        val msg = MessageEntity(
            messageId = msgId,
            conversation = MeshService.BROADCAST_CONVERSATION,
            fromNodeId = fromNodeId,
            fromName = fromName,
            text = text,
            timestamp = System.currentTimeMillis(),
            outgoing = false,
            status = MessageStatus.RECEIVED,
            verified = true
        )
        repository.saveMessage(msg)
    }

    // ---- identity backup & restore (Phase 0.3; docs/BACKUP.md) -------------------------

    /** False only for the legacy Keystore-only identity, whose key can never leave the hardware. */
    fun identityExportable(): Boolean =
        service.value?.router?.identity?.let { IdentityStore.isExportable(it) } ?: false

    /** Encrypts this device's identity as a `RIPPLE-BKP:v1:` blob; null if not exportable/bound. */
    fun createBackupBlob(passphrase: String): String? {
        val identity = service.value?.router?.identity ?: return null
        val scalar = IdentityStore.exportScalar(identity) ?: return null
        return runCatching { Backup.createBlob(scalar, identity.publicKeyWire, passphrase) }.getOrNull()
    }

    /** @return null on success (restart to activate), or a human-readable refusal reason. */
    fun restoreIdentity(payload: ByteArray): String? = try {
        IdentityStore.installRestored(getApplication(), Backup.scalarOf(payload), Backup.publicKeyOf(payload))
        null
    } catch (e: Exception) {
        e.message ?: "restore refused"
    }

    // Diagnostics
    fun linkInfos(): List<LinkInfo> = service.value?.linkInfos() ?: emptyList()
    fun diagnosticsHeader(): String = service.value?.diagnosticsHeader() ?: "Ripple diagnostics (service not bound)"
    fun setLoopback(enabled: Boolean) { service.value?.setLoopback(enabled) }

    fun clearAllData() = viewModelScope.launch {
        repository.clearAllData()
        app.ripple.mesh.data.VerifiedPeers.clearAll(getApplication())
    }

    override fun onCleared() {
        opusPlayer.release()
        opusRecorder.cancelRecording()
        runCatching { getApplication<Application>().unbindService(connection) }
    }
}
