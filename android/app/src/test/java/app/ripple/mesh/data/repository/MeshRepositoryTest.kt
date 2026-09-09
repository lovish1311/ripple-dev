package app.ripple.mesh.data.repository

import app.ripple.mesh.data.ConversationSummary
import app.ripple.mesh.data.MessageDao
import app.ripple.mesh.data.MessageEntity
import app.ripple.mesh.data.MessageStatus
import app.ripple.mesh.data.PeerDao
import app.ripple.mesh.data.PeerEntity
import app.ripple.mesh.data.SosBeaconDao
import app.ripple.mesh.data.SosBeaconEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeshRepositoryTest {

    private lateinit var fakeMessageDao: FakeMessageDao
    private lateinit var fakePeerDao: FakePeerDao
    private lateinit var fakeSosBeaconDao: FakeSosBeaconDao
    private lateinit var repository: MeshRepositoryImpl

    @Before
    fun setUp() {
        fakeMessageDao = FakeMessageDao()
        fakePeerDao = FakePeerDao()
        fakeSosBeaconDao = FakeSosBeaconDao()
        repository = MeshRepositoryImpl(
            messageDao = fakeMessageDao,
            peerDao = fakePeerDao,
            sosBeaconDao = fakeSosBeaconDao,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testSaveAndObserveMessage() = runTest {
        val msg = MessageEntity(
            messageId = "msg1",
            conversation = "peer1",
            fromNodeId = "peer1",
            fromName = "Alice",
            text = "Hello Ripple",
            timestamp = 1000L,
            outgoing = false,
            status = MessageStatus.RECEIVED,
            verified = true
        )

        repository.saveMessage(msg)
        val messages = repository.observeMessages("peer1").first()

        assertEquals(1, messages.size)
        assertEquals("Hello Ripple", messages[0].text)
    }

    @Test
    fun testUpdateMessageStatus() = runTest {
        val msg = MessageEntity(
            messageId = "msg2",
            conversation = "peer2",
            fromNodeId = "self",
            fromName = "Self",
            text = "Ping",
            timestamp = 2000L,
            outgoing = true,
            status = MessageStatus.PENDING,
            verified = true
        )

        repository.saveMessage(msg)
        repository.updateMessageStatus("msg2", MessageStatus.DELIVERED)

        val messages = repository.observeMessages("peer2").first()
        assertEquals(MessageStatus.DELIVERED, messages[0].status)
    }

    @Test
    fun testSaveAndObservePeers() = runTest {
        val peer = PeerEntity(
            nodeId = "peer1",
            publicKeyWire = byteArrayOf(1, 2, 3),
            name = "Bob",
            lastSeen = 5000L,
            hops = 1
        )

        repository.savePeers(listOf(peer))
        val peers = repository.observePeers().first()

        assertEquals(1, peers.size)
        assertEquals("Bob", peers[0].name)
    }

    @Test
    fun testSaveAndObserveSosBeacon() = runTest {
        val sos = SosBeaconEntity(
            messageId = "sos1",
            fromNodeId = "peer3",
            fromName = "Charlie",
            text = "Need help!",
            latE7 = 12345678,
            lngE7 = 87654321,
            accuracyMeters = 5,
            verified = true,
            timestamp = 9000L
        )

        repository.saveSosBeacon(sos)
        val beacons = repository.observeSosBeacons().first()

        assertEquals(1, beacons.size)
        assertEquals("Need help!", beacons[0].text)
    }

    // Fake DAOs for unit testing repository behavior without Android SDK database bindings
    private class FakeMessageDao : MessageDao {
        private val messages = mutableMapOf<String, MessageEntity>()

        override suspend fun upsert(message: MessageEntity) {
            messages[message.messageId] = message
        }

        override suspend fun setStatus(messageId: String, status: MessageStatus) {
            messages[messageId]?.let {
                messages[messageId] = it.copy(status = status)
            }
        }

        override fun observeConversation(conversation: String): Flow<List<MessageEntity>> {
            return flowOf(messages.values.filter { it.conversation == conversation })
        }

        override fun observeConversations(): Flow<List<ConversationSummary>> {
            return flowOf(emptyList())
        }

        override suspend fun markRead(conversation: String) {
            messages.values.filter { it.conversation == conversation && !it.outgoing }.forEach {
                messages[it.messageId] = it.copy(status = MessageStatus.DELIVERED)
            }
        }

        override suspend fun exists(messageId: String): Int = if (messages.containsKey(messageId)) 1 else 0
    }

    private class FakePeerDao : PeerDao {
        private val peers = mutableMapOf<String, PeerEntity>()

        override suspend fun upsertAll(peers: List<PeerEntity>) {
            peers.forEach { this.peers[it.nodeId] = it }
        }

        override fun observeAll(): Flow<List<PeerEntity>> = flowOf(peers.values.toList())

        override suspend fun all(): List<PeerEntity> = peers.values.toList()

        override fun observe(nodeId: String): Flow<PeerEntity?> = flowOf(peers[nodeId])
    }

    private class FakeSosBeaconDao : SosBeaconDao {
        private val beacons = mutableMapOf<String, SosBeaconEntity>()

        override suspend fun upsert(beacon: SosBeaconEntity) {
            beacons[beacon.messageId] = beacon
        }

        override fun observeAll(): Flow<List<SosBeaconEntity>> = flowOf(beacons.values.toList())

        override suspend fun prune(cutoff: Long) {
            beacons.entries.removeIf { it.value.timestamp < cutoff }
        }
    }
}
