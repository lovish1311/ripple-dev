package app.ripple.mesh.data.repository

import app.ripple.mesh.data.ConversationSummary
import app.ripple.mesh.data.MessageDao
import app.ripple.mesh.data.MessageEntity
import app.ripple.mesh.data.MessageStatus
import app.ripple.mesh.data.PeerDao
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.data.RelayDao
import app.ripple.mesh.data.SosBeaconDao
import app.ripple.mesh.data.SosBeaconEntity
import app.ripple.mesh.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val peerDao: PeerDao,
    private val relayDao: RelayDao,
    private val sosBeaconDao: SosBeaconDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MeshRepository {

    override fun observeConversations(): Flow<List<ConversationSummary>> = messageDao.observeConversations()

    override fun observePeers(): Flow<List<PeerEntity>> = peerDao.observeAll()

    override fun observeSosBeacons(): Flow<List<SosBeaconEntity>> = sosBeaconDao.observeAll()

    override fun observeMessages(conversation: String): Flow<List<MessageEntity>> = messageDao.observeConversation(conversation)

    override fun observePeer(nodeIdHex: String): Flow<PeerEntity?> = peerDao.observe(nodeIdHex)

    override suspend fun markRead(conversation: String) = withContext(ioDispatcher) {
        messageDao.markRead(conversation)
    }

    override suspend fun saveMessage(message: MessageEntity) = withContext(ioDispatcher) {
        messageDao.upsert(message)
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) = withContext(ioDispatcher) {
        messageDao.setStatus(messageId, status)
    }

    override suspend fun savePeers(peers: List<PeerEntity>) = withContext(ioDispatcher) {
        peerDao.upsertAll(peers)
    }

    override suspend fun saveSosBeacon(beacon: SosBeaconEntity) = withContext(ioDispatcher) {
        sosBeaconDao.upsert(beacon)
    }

    override suspend fun deleteMessage(messageId: String) = withContext(ioDispatcher) {
        messageDao.deleteMessage(messageId)
    }

    override suspend fun editMessage(messageId: String, newText: String) = withContext(ioDispatcher) {
        messageDao.updateMessageText(messageId, newText, isEdited = true)
    }

    override suspend fun markDeletedForEveryone(messageId: String) = withContext(ioDispatcher) {
        messageDao.markDeletedForEveryone(messageId)
    }

    override suspend fun getMessage(messageId: String): MessageEntity? = withContext(ioDispatcher) {
        messageDao.getMessage(messageId)
    }

    override suspend fun clearAllData() = withContext(ioDispatcher) {
        messageDao.clearAll()
        peerDao.clearAll()
        relayDao.clearAll()
        sosBeaconDao.clearAll()
    }
}
