package app.ripple.mesh.data.repository

import app.ripple.mesh.data.ConversationSummary
import app.ripple.mesh.data.MessageEntity
import app.ripple.mesh.data.MessageStatus
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.data.SosBeaconEntity
import kotlinx.coroutines.flow.Flow

interface MeshRepository {
    fun observeConversations(): Flow<List<ConversationSummary>>
    fun observePeers(): Flow<List<PeerEntity>>
    fun observeSosBeacons(): Flow<List<SosBeaconEntity>>
    fun observeActiveSosBeacons(): Flow<List<SosBeaconEntity>>
    fun observeMessages(conversation: String): Flow<List<MessageEntity>>
    fun observePeer(nodeIdHex: String): Flow<PeerEntity?>
    suspend fun markRead(conversation: String)
    suspend fun saveMessage(message: MessageEntity)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun savePeers(peers: List<PeerEntity>)
    suspend fun saveSosBeacon(beacon: SosBeaconEntity)
    suspend fun setSosBeaconAcknowledged(messageId: String, acknowledged: Boolean = true)
    suspend fun acknowledgeAllSosBeacons()
    suspend fun deleteSosBeacon(messageId: String)
    suspend fun deleteMessage(messageId: String)
    suspend fun editMessage(messageId: String, newText: String)
    suspend fun markDeletedForEveryone(messageId: String)
    suspend fun getMessage(messageId: String): MessageEntity?
    suspend fun clearAllData()
}
