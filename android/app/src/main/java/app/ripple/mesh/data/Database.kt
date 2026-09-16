package app.ripple.mesh.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

enum class MessageStatus { PENDING, SENT, DELIVERED, READ, RECEIVED, FAILED }

/**
 * One chat message. `conversation` is "broadcast" for the public channel, or the
 * hex NodeId of the other party for a direct conversation.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversation", "timestamp"]),
        Index(value = ["status"])
    ]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,          // hex
    val conversation: String,
    val fromNodeId: String,                     // hex
    val fromName: String?,
    val text: String,
    val timestamp: Long,
    val outgoing: Boolean,
    val status: MessageStatus,
    val verified: Boolean,
    val isEdited: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val isForwarded: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val voicePath: String? = null,
    val voiceDurationMs: Int? = null,
)

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val nodeId: String,             // hex
    val publicKeyWire: ByteArray,
    val name: String,
    val lastSeen: Long,
    val hops: Int,
    val avatar: String? = null,
)

/** Relay-store persistence so store-and-forward survives process death. */
@Entity(tableName = "relay_packets")
data class RelayPacketEntity(
    @PrimaryKey val messageId: String,
    val bytes: ByteArray,
    val expiresAt: Long,
)

@Entity(
    tableName = "sos_beacons",
    indices = [
        Index(value = ["isAcknowledged", "timestamp"])
    ]
)
data class SosBeaconEntity(
    @PrimaryKey val messageId: String,           // hex
    val fromNodeId: String,                      // hex
    val fromName: String?,
    val text: String,
    val latE7: Int?,
    val lngE7: Int?,
    val accuracyMeters: Int?,
    val verified: Boolean,
    val timestamp: Long,
    val isAcknowledged: Boolean = false,
    val voicePath: String? = null,
    val voiceDurationMs: Int? = null,
)

data class ConversationSummary(val conversation: String, val lastText: String, val lastTimestamp: Long, val unread: Int)

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun setStatus(messageId: String, status: MessageStatus)

    @Query("SELECT * FROM messages WHERE conversation = :conversation ORDER BY timestamp ASC")
    fun observeConversation(conversation: String): Flow<List<MessageEntity>>

    @Query(
        """SELECT conversation,
                  (SELECT text FROM messages m2 WHERE m2.conversation = m.conversation ORDER BY timestamp DESC LIMIT 1) AS lastText,
                  MAX(timestamp) AS lastTimestamp,
                  SUM(CASE WHEN status = 'RECEIVED' AND outgoing = 0 THEN 1 ELSE 0 END) AS unread
           FROM messages m GROUP BY conversation ORDER BY lastTimestamp DESC"""
    )
    fun observeConversations(): Flow<List<ConversationSummary>>

    @Query("UPDATE messages SET status = 'READ' WHERE conversation = :conversation AND outgoing = 0 AND status = 'RECEIVED'")
    suspend fun markRead(conversation: String)

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'RECEIVED' AND outgoing = 0")
    suspend fun countUnread(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE messageId = :messageId")
    suspend fun exists(messageId: String): Int

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("UPDATE messages SET text = :newText, isEdited = :isEdited WHERE messageId = :messageId")
    suspend fun updateMessageText(messageId: String, newText: String, isEdited: Boolean = true)

    @Query("UPDATE messages SET deletedForEveryone = 1 WHERE messageId = :messageId")
    suspend fun markDeletedForEveryone(messageId: String)

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessage(messageId: String): MessageEntity?

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}

@Dao
interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(peers: List<PeerEntity>)

    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun observeAll(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers")
    suspend fun all(): List<PeerEntity>

    @Query("SELECT * FROM peers WHERE nodeId = :nodeId")
    fun observe(nodeId: String): Flow<PeerEntity?>

    @Query("DELETE FROM peers")
    suspend fun clearAll()
}

@Dao
interface RelayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(packets: List<RelayPacketEntity>)

    @Query("SELECT * FROM relay_packets WHERE expiresAt > :now")
    suspend fun live(now: Long): List<RelayPacketEntity>

    @Query("DELETE FROM relay_packets WHERE expiresAt <= :now")
    suspend fun purge(now: Long)

    @Query("DELETE FROM relay_packets")
    suspend fun clearAll()
}

@Dao
interface SosBeaconDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(beacon: SosBeaconEntity)

    @Query("SELECT * FROM sos_beacons WHERE isAcknowledged = 0 ORDER BY timestamp DESC")
    fun observeActive(): Flow<List<SosBeaconEntity>>

    @Query("SELECT * FROM sos_beacons ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SosBeaconEntity>>

    @Query("UPDATE sos_beacons SET isAcknowledged = :acknowledged WHERE messageId = :messageId")
    suspend fun setAcknowledged(messageId: String, acknowledged: Boolean = true)

    @Query("UPDATE sos_beacons SET isAcknowledged = 1 WHERE isAcknowledged = 0")
    suspend fun acknowledgeAll()

    @Query("DELETE FROM sos_beacons WHERE messageId = :messageId")
    suspend fun delete(messageId: String)

    @Query("DELETE FROM sos_beacons WHERE timestamp < :cutoff")
    suspend fun prune(cutoff: Long)

    @Query("DELETE FROM sos_beacons")
    suspend fun clearAll()
}

/** v1 → v2: add the `sos_beacons` history table. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sos_beacons` (" +
                "`messageId` TEXT NOT NULL, " +
                "`fromNodeId` TEXT NOT NULL, " +
                "`fromName` TEXT, " +
                "`text` TEXT NOT NULL, " +
                "`latE7` INTEGER, " +
                "`lngE7` INTEGER, " +
                "`accuracyMeters` INTEGER, " +
                "`verified` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "PRIMARY KEY(`messageId`))"
        )
    }
}

/** v2 → v3: add composite indexes on messages(conversation, timestamp) and messages(status). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversation_timestamp` ON `messages` (`conversation`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_status` ON `messages` (`status`)")
    }
}

/** v3 → v4: add isEdited and deletedForEveryone columns to messages table. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `isEdited` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `deletedForEveryone` INTEGER NOT NULL DEFAULT 0")
    }
}

/** v4 → v5: add isForwarded column to messages table. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `isForwarded` INTEGER NOT NULL DEFAULT 0")
    }
}

/** v5 → v6: add isAcknowledged column to sos_beacons and index on (isAcknowledged, timestamp). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `sos_beacons` ADD COLUMN `isAcknowledged` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sos_beacons_isAcknowledged_timestamp` ON `sos_beacons` (`isAcknowledged`, `timestamp`)")
    }
}

/** v6 → v7: add replyToMessageId, replyToText, and replyToSender columns to messages table. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToMessageId` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToText` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `replyToSender` TEXT DEFAULT NULL")
    }
}

/** v7 → v8: add avatar column to peers table. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `peers` ADD COLUMN `avatar` TEXT DEFAULT NULL")
    }
}

/** v8 → v9: add voicePath and voiceDurationMs to messages and sos_beacons tables. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `voicePath` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `messages` ADD COLUMN `voiceDurationMs` INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE `sos_beacons` ADD COLUMN `voicePath` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `sos_beacons` ADD COLUMN `voiceDurationMs` INTEGER DEFAULT NULL")
    }
}

@Database(entities = [MessageEntity::class, PeerEntity::class, RelayPacketEntity::class, SosBeaconEntity::class], version = 9, exportSchema = false)
abstract class RippleDatabase : RoomDatabase() {
    abstract fun messages(): MessageDao
    abstract fun peers(): PeerDao
    abstract fun relay(): RelayDao
    abstract fun sos(): SosBeaconDao

    companion object {
        /** Received SOS beacons are retained for ~90 days before being pruned. */
        const val SOS_RETENTION_MS = 90L * 24 * 3600 * 1000

        @Volatile private var instance: RippleDatabase? = null
        fun get(context: Context): RippleDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, RippleDatabase::class.java, "ripple.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
