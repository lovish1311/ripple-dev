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
    fun observeMessages(conversation: String): Flow<List<MessageEntity>>
    fun observePeer(nodeIdHex: String): Flow<PeerEntity?>
    suspend fun markRead(conversation: String)
    suspend fun saveMessage(message: MessageEntity)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun savePeers(peers: List<PeerEntity>)
    suspend fun saveSosBeacon(beacon: SosBeaconEntity)
}
