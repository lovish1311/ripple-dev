import Foundation
import SwiftData

enum MessageStatus: String, Codable { case pending, sent, delivered, received, failed }

enum SosStatus: String, Codable, CaseIterable {
    case active
    case acknowledged
    case resolved
}

/// One chat message. `conversation` is "broadcast" for the public channel, or the
/// hex NodeId of the other party for a direct conversation.
@Model
final class MessageRecord {
    @Attribute(.unique) var messageId: String
    var conversation: String
    var fromNodeId: String
    var fromName: String?
    var text: String
    var timestamp: Date
    var outgoing: Bool
    var statusRaw: String
    var verified: Bool
    var voiceBytes: Data?
    var voiceDurationMs: Int?

    var status: MessageStatus {
        get { MessageStatus(rawValue: statusRaw) ?? .received }
        set { statusRaw = newValue.rawValue }
    }

    var isVoice: Bool {
        voiceBytes != nil && !(voiceBytes?.isEmpty ?? true)
    }

    init(messageId: String, conversation: String, fromNodeId: String, fromName: String?, text: String,
         timestamp: Date, outgoing: Bool, status: MessageStatus, verified: Bool,
         voiceBytes: Data? = nil, voiceDurationMs: Int? = nil) {
        self.messageId = messageId; self.conversation = conversation; self.fromNodeId = fromNodeId; self.fromName = fromName
        self.text = text; self.timestamp = timestamp; self.outgoing = outgoing; self.statusRaw = status.rawValue; self.verified = verified
        self.voiceBytes = voiceBytes; self.voiceDurationMs = voiceDurationMs
    }
}

@Model
final class PeerRecord {
    @Attribute(.unique) var nodeId: String
    var publicKeyWire: Data
    var name: String
    var lastSeen: Date
    var hops: Int

    init(nodeId: String, publicKeyWire: Data, name: String, lastSeen: Date, hops: Int) {
        self.nodeId = nodeId; self.publicKeyWire = publicKeyWire; self.name = name; self.lastSeen = lastSeen; self.hops = hops
    }
}

/// Relay-store persistence so store-and-forward survives process death.
@Model
final class RelayPacketRecord {
    @Attribute(.unique) var messageId: String
    var bytes: Data
    var expiresAt: Date

    init(messageId: String, bytes: Data, expiresAt: Date) {
        self.messageId = messageId; self.bytes = bytes; self.expiresAt = expiresAt
    }
}

/// Received SOS beacon history (PROTOCOL.md §2.2), retained for ~90 days.
@Model
final class SosRecord {
    @Attribute(.unique) var messageId: String
    var fromNodeId: String
    var fromName: String?
    var text: String
    var latE7: Int32?
    var lngE7: Int32?
    var accuracyMeters: Int?
    var verified: Bool
    var timestamp: Date
    var statusRaw: String = "active"
    var voiceBytes: Data?
    var voiceDurationMs: Int?

    var status: SosStatus {
        get { SosStatus(rawValue: statusRaw) ?? .active }
        set { statusRaw = newValue.rawValue }
    }

    var hasLocation: Bool { latE7 != nil && lngE7 != nil }
    var hasVoice: Bool { voiceBytes != nil && !(voiceBytes?.isEmpty ?? true) }

    init(messageId: String, fromNodeId: String, fromName: String?, text: String,
         latE7: Int32?, lngE7: Int32?, accuracyMeters: Int?, verified: Bool, timestamp: Date,
         status: SosStatus = .active, voiceBytes: Data? = nil, voiceDurationMs: Int? = nil) {
        self.messageId = messageId; self.fromNodeId = fromNodeId; self.fromName = fromName
        self.text = text; self.latE7 = latE7; self.lngE7 = lngE7; self.accuracyMeters = accuracyMeters
        self.verified = verified; self.timestamp = timestamp
        self.statusRaw = status.rawValue; self.voiceBytes = voiceBytes; self.voiceDurationMs = voiceDurationMs
    }
}

enum Persistence {
    static let broadcastConversation = "broadcast"

    static func container() -> ModelContainer {
        let schema = Schema([MessageRecord.self, PeerRecord.self, RelayPacketRecord.self, SosRecord.self])
        do {
            return try ModelContainer(for: schema, configurations: [ModelConfiguration(schema: schema)])
        } catch {
            fatalError("Could not create SwiftData container: \(error)")
        }
    }
}
