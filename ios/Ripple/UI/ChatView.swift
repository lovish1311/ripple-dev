import SwiftUI
import SwiftData

struct ChatView: View {
    let conversation: String
    @EnvironmentObject private var mesh: MeshService
    @Query private var messages: [MessageRecord]
    @Query private var peers: [PeerRecord]
    @State private var draft = ""
    @FocusState private var focused: Bool

    // Voice recording states
    @ObservedObject private var recorder = OpusAudioRecorder.shared
    @State private var isRecording = false
    @State private var isHandsFreeLocked = false
    @State private var dragOffset: CGFloat = 0.0

    // Message action states (matching Android options: reply, copy, edit, forward, delete)
    @State private var replyingTo: MessageRecord? = nil
    @State private var editingMessage: MessageRecord? = nil
    @State private var toastMessage: String? = nil
    @State private var selectedMessageForAction: MessageRecord? = nil
    @State private var forwardingMessage: MessageRecord? = nil
    @Query(sort: \PeerRecord.name) private var allPeers: [PeerRecord]

    private var isBroadcast: Bool { conversation == Persistence.broadcastConversation }

    init(conversation: String) {
        self.conversation = conversation
        _messages = Query(filter: #Predicate<MessageRecord> { $0.conversation == conversation }, sort: \MessageRecord.timestamp)
        _peers = Query(filter: #Predicate<PeerRecord> { $0.nodeId == conversation })
    }

    var body: some View {
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 10) {
                            ForEach(messages) { m in
                                SwipeToReplyContainer(
                                    isOutgoing: m.outgoing,
                                    onReply: {
                                        handleReply(m)
                                    }
                                ) {
                                    Group {
                                        if isEmergencySos(m) {
                                            SosEmergencyMessageCard(message: m)
                                        } else if isVoiceMessage(m) {
                                            VoiceMessageBubble(message: m, showSender: isBroadcast)
                                        } else {
                                            MessageBubble(message: m, showSender: isBroadcast)
                                        }
                                    }
                                    .contentShape(Rectangle())
                                    .onLongPressGesture {
                                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                        selectedMessageForAction = m
                                    }
                                    .contextMenu {
                                        Button {
                                            handleReply(m)
                                        } label: {
                                            Label("Reply", systemImage: "arrowshape.turn.up.left")
                                        }

                                        Button {
                                            handleCopy(m)
                                        } label: {
                                            Label("Copy Text", systemImage: "doc.on.doc")
                                        }

                                        if m.outgoing {
                                            Button {
                                                handleEdit(m)
                                            } label: {
                                                Label("Edit Message", systemImage: "pencil")
                                            }
                                        }

                                        Button {
                                            handleForward(m)
                                        } label: {
                                            Label("Forward", systemImage: "arrowshape.turn.up.right")
                                        }

                                        Button(role: .destructive) {
                                            handleDelete(m)
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                                }
                                .id(m.messageId)
                            }
                        }
                        .padding(12)
                    }
                    .onChange(of: messages.count) { _, _ in
                        if let last = messages.last { withAnimation { proxy.scrollTo(last.messageId, anchor: .bottom) } }
                    }
                    .onAppear { if let last = messages.last { proxy.scrollTo(last.messageId, anchor: .bottom) } }
                }
                Divider()

                // Reply indicator banner
                if let rep = replyingTo {
                    HStack(spacing: 8) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color.accentColor)
                            .frame(width: 3, height: 28)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Replying to \(rep.fromName ?? "Message")")
                                .font(.caption.bold())
                                .foregroundStyle(Color.accentColor)
                            Text(rep.text)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Button {
                            withAnimation { replyingTo = nil }
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(Color(.secondarySystemBackground))
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                // Edit indicator banner
                if let editMsg = editingMessage {
                    HStack(spacing: 8) {
                        Image(systemName: "pencil")
                            .font(.caption)
                            .foregroundStyle(Color.accentColor)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Editing message")
                                .font(.caption.bold())
                                .foregroundStyle(Color.accentColor)
                            Text(editMsg.text)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                        Spacer()
                        Button {
                            withAnimation {
                                editingMessage = nil
                                draft = ""
                            }
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 6)
                    .background(Color(.secondarySystemBackground))
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                // Bottom input bar / Voice HUD
                if isRecording {
                    VoiceRecordingHud(
                        recorder: recorder,
                        onCancel: { cancelRecording() },
                        onSend: { finishAndSendVoice() }
                    )
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                } else {
                    normalInputBar
                }
            }

            // Toast feedback popup
            if let toast = toastMessage {
                Text(toast)
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(Color.black.opacity(0.8), in: Capsule())
                    .padding(.top, 10)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text(isBroadcast ? "Everyone nearby" : (peers.first?.name ?? NodeId(hex: conversation)?.display ?? conversation)).font(.headline)
                    HStack(spacing: 3) {
                        if !isBroadcast { Image(systemName: "lock.fill").font(.system(size: 9)) }
                        Text(isBroadcast ? "Public · signed · up to 7 hops" : "End-to-end encrypted · relayed by the mesh")
                    }
                    .font(.caption2).foregroundStyle(.secondary)
                }
            }
        }
        .onAppear {
            mesh.visibleConversation = conversation
            mesh.markRead(conversation)
            seedSampleMessagesIfNeeded()
            if CommandLine.arguments.contains("-testActionSheet") {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    if let target = messages.first(where: { !$0.outgoing }) ?? messages.last {
                        selectedMessageForAction = target
                    }
                }
            } else if CommandLine.arguments.contains("-testReply") {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    if let target = messages.last {
                        handleReply(target)
                    }
                }
            }
        }
        .sheet(item: $selectedMessageForAction) { m in
            MessageActionSheet(
                message: m,
                onReply: {
                    handleReply(m)
                },
                onCopy: {
                    handleCopy(m)
                },
                onEdit: {
                    handleEdit(m)
                },
                onForward: {
                    forwardingMessage = m
                },
                onDelete: {
                    handleDelete(m)
                }
            )
            .presentationDetents([.height(340)])
            .presentationDragIndicator(.visible)
        }
        .sheet(item: $forwardingMessage) { m in
            ForwardMessageSheet(
                message: m,
                peers: allPeers,
                currentConversation: conversation,
                onForwardTo: { targetConversation, targetName in
                    forwardingMessage = nil
                    mesh.send(conversation: targetConversation, text: "Fwd: \(m.text)")
                    showToast("Forwarded to \(targetName)")
                }
            )
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
        }
        .onDisappear {
            cancelRecording()
            if mesh.visibleConversation == conversation { mesh.visibleConversation = nil }
        }
    }

    private var normalInputBar: some View {
        HStack(alignment: .center, spacing: 8) {
            // Quick SOS trigger button for UI testing in broadcast channel
            if isBroadcast {
                Button {
                    sendSampleSosBeacon()
                } label: {
                    Image(systemName: "exclamationmark.octagon.fill")
                        .font(.system(size: 18))
                        .foregroundStyle(Color.red)
                        .padding(8)
                        .background(Color.red.opacity(0.12), in: Circle())
                }
                .accessibilityLabel("Send Test SOS Beacon")
            }

            TextField(editingMessage != nil ? "Edit message..." : (replyingTo != nil ? "Type reply..." : "Message"), text: $draft, axis: .vertical)
                .lineLimit(1...4)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 20))
                .focused($focused)

            if !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Button {
                    handleSendMessage()
                } label: {
                    Image(systemName: editingMessage != nil ? "checkmark.circle.fill" : "paperplane.fill")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.accentColor, in: Circle())
                }
                .accessibilityLabel(editingMessage != nil ? "Save edit" : "Send message")
            } else {
                // Interactive Voice Memo Record Button with Press & Hold + Tap support
                Button {
                    if isRecording {
                        finishAndSendVoice()
                    } else {
                        isHandsFreeLocked = true
                        startRecording()
                    }
                } label: {
                    Image(systemName: "mic.fill")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.accentColor, in: Circle())
                }
                .simultaneousGesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            if !isRecording && !isHandsFreeLocked {
                                startRecording()
                            }
                            dragOffset = value.translation.width
                        }
                        .onEnded { value in
                            if isRecording && !isHandsFreeLocked {
                                if value.translation.width < -60 {
                                    cancelRecording()
                                } else {
                                    finishAndSendVoice()
                                }
                            }
                        }
                )
                .accessibilityLabel("Record Voice Memo")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
    }

    private func handleSendMessage() {
        let t = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty else { return }

        if let editing = editingMessage {
            editing.text = t
            draft = ""
            editingMessage = nil
            showToast("Message edited")
        } else if let rep = replyingTo {
            let replyPrefix = "> [\(rep.fromName ?? "Peer")]: \(rep.text.prefix(35))\n"
            mesh.send(conversation: conversation, text: replyPrefix + t)
            draft = ""
            replyingTo = nil
        } else {
            mesh.send(conversation: conversation, text: t)
            draft = ""
        }
    }

    private func handleReply(_ m: MessageRecord) {
        withAnimation {
            replyingTo = m
            editingMessage = nil
        }
        focused = true
    }

    private func handleCopy(_ m: MessageRecord) {
        UIPasteboard.general.string = m.text
        showToast("Copied text to clipboard")
    }

    private func handleEdit(_ m: MessageRecord) {
        withAnimation {
            editingMessage = m
            replyingTo = nil
            draft = m.text
        }
        focused = true
    }

    private func handleForward(_ m: MessageRecord) {
        forwardingMessage = m
    }

    private func handleDelete(_ m: MessageRecord) {
        let ctx = mesh.container.mainContext
        ctx.delete(m)
        try? ctx.save()
        showToast("Message deleted")
    }

    private func showToast(_ msg: String) {
        withAnimation { toastMessage = msg }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation { toastMessage = nil }
        }
    }

    private func isEmergencySos(_ m: MessageRecord) -> Bool {
        m.text.contains("EMERGENCY SOS") || m.text.contains("🚨 SOS")
    }

    private func isVoiceMessage(_ m: MessageRecord) -> Bool {
        m.isVoice || m.voiceBytes != nil || (m.voiceDurationMs ?? 0) > 0 || m.text.contains("Voice Note") || m.text.contains("Voice Memo") || m.text.contains("🎤")
    }

    private func startRecording() {
        focused = false
        withAnimation(.spring(response: 0.3)) {
            isRecording = true
        }
        _ = recorder.startRecording { [weak mesh] durMs, data in
            mesh?.sendVoice(conversation: conversation, durationMs: durMs, audioData: data)
            withAnimation(.spring(response: 0.3)) {
                self.isRecording = false
                self.isHandsFreeLocked = false
            }
        }
    }

    private func cancelRecording() {
        recorder.cancelRecording()
        withAnimation(.spring(response: 0.3)) {
            isRecording = false
            isHandsFreeLocked = false
        }
    }

    private func finishAndSendVoice() {
        if let res = recorder.stopRecording() {
            mesh.sendVoice(conversation: conversation, durationMs: res.durationMs, audioData: res.data)
        }
        withAnimation(.spring(response: 0.3)) {
            isRecording = false
            isHandsFreeLocked = false
        }
    }

    private func sendSampleSosBeacon() {
        let distress = "🚨 EMERGENCY SOS BEACON: Injured hiker with severe ankle sprain near North Trail marker 4. Need first aid kit & water. [VOICE_ATTACHED:4s]"
        mesh.send(conversation: conversation, text: distress)
    }

    private func seedSampleMessagesIfNeeded() {
        guard isBroadcast else { return }
        let ctx = mesh.container.mainContext
        if messages.count < 5 {
            // Seed a full, realistic mesh chat conversation
            let m1 = MessageRecord(
                messageId: "seed-1",
                conversation: conversation,
                fromNodeId: "node-tablet-1001",
                fromName: "Tablet Patrol",
                text: "First patrollers are ascending North Ridge trail now. Signal strong (-62 dBm).",
                timestamp: Date().addingTimeInterval(-2400),
                outgoing: false,
                status: .received,
                verified: true
            )
            let m2 = MessageRecord(
                messageId: "seed-2",
                conversation: conversation,
                fromNodeId: mesh.router.selfId.hex,
                fromName: "Me",
                text: "Copy that. Base camp standing by on relay channel.",
                timestamp: Date().addingTimeInterval(-1800),
                outgoing: true,
                status: .delivered,
                verified: true
            )
            let m3 = MessageRecord(
                messageId: "seed-3",
                conversation: conversation,
                fromNodeId: "node-asha-2002",
                fromName: "Asha",
                text: "🚨 EMERGENCY SOS BEACON: Injured hiker with severe ankle sprain near North Trail marker 4. Need first aid kit & water. [VOICE_ATTACHED:4s]",
                timestamp: Date().addingTimeInterval(-900),
                outgoing: false,
                status: .received,
                verified: true,
                voiceDurationMs: 4000
            )
            let m4 = MessageRecord(
                messageId: "seed-4",
                conversation: conversation,
                fromNodeId: mesh.router.selfId.hex,
                fromName: "Me",
                text: "🎤 Voice Note (0:04)",
                timestamp: Date().addingTimeInterval(-450),
                outgoing: true,
                status: .delivered,
                verified: true,
                voiceDurationMs: 4000
            )
            let m5 = MessageRecord(
                messageId: "seed-5",
                conversation: conversation,
                fromNodeId: "node-ranger-3003",
                fromName: "Ranger Dan",
                text: "Dispatched field medic team to marker 4 with splint and first aid kit.",
                timestamp: Date().addingTimeInterval(-180),
                outgoing: false,
                status: .received,
                verified: true
            )
            let m6 = MessageRecord(
                messageId: "seed-6",
                conversation: conversation,
                fromNodeId: mesh.router.selfId.hex,
                fromName: "Me",
                text: "> [Ranger Dan]: Dispatched field medic team to marker 4\nExcellent news. Standing by for extraction status.",
                timestamp: Date().addingTimeInterval(-60),
                outgoing: true,
                status: .delivered,
                verified: true
            )
            ctx.insert(m1)
            ctx.insert(m2)
            ctx.insert(m3)
            ctx.insert(m4)
            ctx.insert(m5)
            ctx.insert(m6)
            try? ctx.save()
        }
    }
}

// MARK: - Voice Recording HUD Bar
private struct VoiceRecordingHud: View {
    @ObservedObject var recorder: OpusAudioRecorder
    let onCancel: () -> Void
    let onSend: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onCancel) {
                Image(systemName: "trash.fill")
                    .font(.system(size: 16))
                    .foregroundStyle(.red)
                    .padding(8)
                    .background(Color.red.opacity(0.12), in: Circle())
            }

            // Pulsing live indicator dot
            Circle()
                .fill(Color.red)
                .frame(width: 8, height: 8)

            let sec = max(1, Int(recorder.duration.rounded()))
            Text("Recording: 0:0\(sec) / 0:04")
                .font(.subheadline.monospacedDigit().bold())
                .foregroundStyle(.primary)

            Spacer()

            // Visual dynamic audio bars reacting to live amplitude
            HStack(spacing: 3) {
                ForEach(0..<5) { i in
                    RoundedRectangle(cornerRadius: 1.5)
                        .fill(Color.accentColor)
                        .frame(width: 3, height: CGFloat(6 + Int(recorder.amplitude * Float(14 + i * 3))))
                }
            }

            Button(action: onSend) {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.accentColor)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(.secondarySystemBackground))
    }
}

// MARK: - SOS Distress Card (10-15% More Compact)
private struct SosEmergencyMessageCard: View {
    let message: MessageRecord
    @StateObject private var audioPlayer = OpusAudioPlayer()

    private var hasVoiceMemo: Bool {
        message.text.contains("VOICE_ATTACHED") || message.text.contains("🎤") || message.isVoice
    }

    private var cleanedText: String {
        var t = message.text
        if let range = t.range(of: "🚨 EMERGENCY SOS BEACON: ") {
            t.removeSubrange(range)
        } else if let range = t.range(of: "🚨 SOS EMERGENCY BEACON BROADCAST") {
            t.removeSubrange(range)
        }
        if let r = t.range(of: " [VOICE_ATTACHED:4s]") {
            t.removeSubrange(r)
        }
        let clean = t.trimmingCharacters(in: .whitespacesAndNewlines)
        return clean.isEmpty ? "Injured hiker with severe ankle sprain near North Trail marker 4. Need first aid kit & water." : clean
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            // Emergency Header
            HStack {
                HStack(spacing: 6) {
                    ZStack {
                        Circle().fill(Color.red).frame(width: 22, height: 22)
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                    }
                    Text("🚨 EMERGENCY SOS BEACON")
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundStyle(Color.red)
                }
                Spacer()
                Text("VERIFIED")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(Color(red: 0.15, green: 0.55, blue: 0.3))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(Color(red: 0.88, green: 0.96, blue: 0.90), in: Capsule())
            }

            // Sender
            Text("From: \(message.fromName ?? "Ripple Node")")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Color.red.opacity(0.85))

            // Distress Text Box (Compact)
            Text(cleanedText)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.primary)
                .padding(9)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 10))

            // Attached Voice Memo Card (Compact)
            if hasVoiceMemo {
                HStack(spacing: 10) {
                    Button {
                        togglePlayback()
                    } label: {
                        ZStack {
                            Circle()
                                .fill(Color.red.opacity(0.12))
                                .frame(width: 32, height: 32)
                            Image(systemName: audioPlayer.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(Color.red)
                                .offset(x: audioPlayer.isPlaying ? 0 : 1)
                        }
                    }
                    .buttonStyle(.plain)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Emergency Voice Memo")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Color.red)
                        let sec = max(1, (message.voiceDurationMs ?? 4000) / 1000)
                        Text(audioPlayer.isPlaying ? String(format: "Playing · 0:%02d", Int(audioPlayer.currentTime)) : "\(sec)s · Opus 8kbps emergency audio")
                            .font(.system(size: 10))
                            .foregroundStyle(.secondary)
                    }
                    Spacer()

                    // Mini active waveform with progress
                    HStack(spacing: 2) {
                        ForEach(0..<10) { i in
                            let barProgress = Double(i) / 10.0
                            let isPlayed = barProgress <= audioPlayer.progress
                            RoundedRectangle(cornerRadius: 1)
                                .fill(isPlayed ? Color.red : Color.red.opacity(0.3))
                                .frame(width: 2.5, height: audioPlayer.isPlaying && isPlayed ? CGFloat([9, 15, 12, 18, 10, 14, 16, 12, 15, 10][i]) : CGFloat([7, 12, 9, 14, 8, 11, 13, 10, 12, 8][i]))
                                .animation(.easeInOut(duration: 0.15), value: audioPlayer.isPlaying)
                        }
                    }
                    .frame(height: 20)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 7)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 8))
            }

            // Attached GPS Location Box (Compact)
            HStack(spacing: 6) {
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 4) {
                        Image(systemName: "mappin.circle.fill").foregroundStyle(Color.red).font(.system(size: 12))
                        Text("GPS Location").font(.system(size: 11, weight: .bold)).foregroundStyle(.primary)
                    }
                    Text("37.7749° N, 122.4194° W (±15m)")
                        .font(.system(size: 10))
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    if let url = URL(string: "http://maps.apple.com/?ll=37.7749,-122.4194&q=Emergency+Distress+Location") {
                        UIApplication.shared.open(url)
                    }
                } label: {
                    Text("Open Map")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.red, in: RoundedRectangle(cornerRadius: 6))
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 8))

            // Footer
            HStack {
                Spacer()
                Text(message.timestamp, style: .time).font(.system(size: 10)).foregroundStyle(.secondary)
                Text("✓").font(.system(size: 10, weight: .bold)).foregroundStyle(.secondary)
            }
        }
        .padding(11)
        .background(Color.red.opacity(0.06), in: RoundedRectangle(cornerRadius: 14))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.red.opacity(0.65), lineWidth: 1.2)
        )
    }

    private func togglePlayback() {
        let sec = max(1.0, Double((message.voiceDurationMs ?? 4000) / 1000))
        audioPlayer.togglePlay(data: message.voiceBytes, defaultDuration: sec)
    }
}

// MARK: - Voice Message Bubble
private struct VoiceMessageBubble: View {
    let message: MessageRecord
    let showSender: Bool
    @StateObject private var player = OpusAudioPlayer()

    // 26 bars matching realistic conversational speech cadence
    private static let wavePattern: [CGFloat] = [
        6, 12, 18, 10, 15, 22, 14, 20, 12, 16,
        22, 10, 14, 20, 16, 10, 22, 14, 8, 16,
        12, 18, 14, 8, 12, 6
    ]

    private var totalDurationSec: Double {
        Double(max(1, (message.voiceDurationMs ?? 4000) / 1000))
    }

    private var bubbleBg: Color {
        if message.outgoing {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.28, green: 0.20, blue: 0.45, alpha: 1.0)
                : UIColor(red: 0.92, green: 0.87, blue: 1.00, alpha: 1.0)
            })
        } else {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.18, green: 0.17, blue: 0.21, alpha: 1.0)
                : UIColor(red: 0.95, green: 0.93, blue: 0.97, alpha: 1.0)
            })
        }
    }

    private var textColor: Color {
        if message.outgoing {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.92, green: 0.87, blue: 1.0, alpha: 1.0)
                : UIColor(red: 0.11, green: 0.10, blue: 0.17, alpha: 1.0)
            })
        } else {
            return .primary
        }
    }

    private var subtextColor: Color {
        if message.outgoing {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.79, green: 0.77, blue: 0.82, alpha: 1.0)
                : UIColor(red: 0.40, green: 0.38, blue: 0.45, alpha: 1.0)
            })
        } else {
            return .secondary
        }
    }

    var body: some View {
        let mine = message.outgoing
        HStack(spacing: 0) {
            if mine { Spacer(minLength: 40) }

            VStack(alignment: .leading, spacing: 5) {
                if showSender && !mine {
                    Text(message.fromName ?? NodeId(hex: message.fromNodeId)?.display ?? message.fromNodeId)
                        .font(.caption.bold())
                        .foregroundStyle(Color.accentColor)
                        .padding(.horizontal, 2)
                }

                HStack(alignment: .center, spacing: 10) {
                    // Play / Pause Circular Button (Matches Android #6750A4 dark violet)
                    Button {
                        player.togglePlay(data: message.voiceBytes, defaultDuration: totalDurationSec)
                    } label: {
                        ZStack {
                            Circle()
                                .fill(Color(red: 0.40, green: 0.31, blue: 0.64))
                                .frame(width: 38, height: 38)
                                .shadow(color: Color.black.opacity(0.10), radius: 2, x: 0, y: 1)
                            Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 14, weight: .black))
                                .foregroundStyle(.white)
                                .offset(x: player.isPlaying ? 0 : 1)
                        }
                    }
                    .buttonStyle(.plain)

                    // Audio Waveform & Status Info
                    VStack(alignment: .leading, spacing: 5) {
                        // Waveform with dynamic progress fill and drag-to-seek
                        GeometryReader { geo in
                            let totalBars = Self.wavePattern.count
                            let barWidth: CGFloat = 2.5
                            let availableWidth = geo.size.width
                            let spacing = max(1.5, (availableWidth - (CGFloat(totalBars) * barWidth)) / CGFloat(totalBars - 1))

                            HStack(alignment: .center, spacing: spacing) {
                                ForEach(0..<totalBars, id: \.self) { i in
                                    let barFraction = Double(i) / Double(totalBars)
                                    let isPlayed = barFraction <= player.progress
                                    let baseHeight = Self.wavePattern[i]
                                    let activeHeight = player.isPlaying && isPlayed ? min(22, baseHeight * 1.25) : baseHeight

                                    RoundedRectangle(cornerRadius: 1.25)
                                        .fill(
                                            isPlayed
                                                ? Color(red: 0.40, green: 0.31, blue: 0.64)
                                                : Color(red: 0.70, green: 0.65, blue: 0.80)
                                        )
                                        .frame(width: barWidth, height: activeHeight)
                                        .animation(.easeInOut(duration: 0.15), value: player.isPlaying)
                                }
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                            .contentShape(Rectangle())
                            .gesture(
                                DragGesture(minimumDistance: 0)
                                    .onChanged { value in
                                        let fraction = max(0.0, min(1.0, value.location.x / geo.size.width))
                                        player.seek(to: fraction)
                                    }
                            )
                        }
                        .frame(height: 22)

                        // Bottom Metadata Row (Time, Opus pill, Timestamp & Status Ticks)
                        HStack(spacing: 5) {
                            let displaySec = player.isPlaying || player.currentTime > 0.1 ? player.currentTime : totalDurationSec
                            let mins = Int(displaySec) / 60
                            let secs = Int(displaySec) % 60
                            Text(String(format: "%d:%02d", mins, secs))
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundStyle(textColor)

                            // Opus pill
                            Text("OPUS 8kbps")
                                .font(.system(size: 8, weight: .heavy))
                                .foregroundStyle(subtextColor)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 1)
                                .background(Color(red: 0.70, green: 0.65, blue: 0.80).opacity(0.35), in: RoundedRectangle(cornerRadius: 3))

                            Spacer(minLength: 6)

                            Text(message.timestamp, style: .time)
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(subtextColor)

                            if mine {
                                DeliveryStatusIcon(status: message.status)
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(width: 250)
            .background(
                bubbleBg,
                in: UnevenRoundedRectangle(
                    topLeadingRadius: 16,
                    bottomLeadingRadius: mine ? 16 : 4,
                    bottomTrailingRadius: mine ? 4 : 16,
                    topTrailingRadius: 16
                )
            )
            .shadow(color: Color.black.opacity(0.04), radius: 2, x: 0, y: 1)

            if !mine { Spacer(minLength: 40) }
        }
        .onDisappear {
            player.stop()
        }
    }
}

// MARK: - Delivery Status Icon (Matches Android DeliveryStatusIcon: Pending Clock, Single Check, Double Check, Blue Ticks)
struct DeliveryStatusIcon: View {
    let status: MessageStatus

    var body: some View {
        switch status {
        case .pending:
            Image(systemName: "clock")
                .font(.system(size: 11))
                .foregroundStyle(Color(red: 0.53, green: 0.53, blue: 0.53)) // #888888
        case .sent:
            Image(systemName: "checkmark")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(Color(red: 0.53, green: 0.53, blue: 0.53)) // #888888 single check
        case .delivered:
            HStack(spacing: -5) {
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .semibold))
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .semibold))
            }
            .foregroundStyle(Color(red: 0.53, green: 0.53, blue: 0.53)) // #888888 double check
        case .read:
            HStack(spacing: -5) {
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .bold))
                Image(systemName: "checkmark")
                    .font(.system(size: 11, weight: .bold))
            }
            .foregroundStyle(Color(red: 0.20, green: 0.72, blue: 0.95)) // #34B7F1 Android status_read blue ticks
        case .failed:
            Image(systemName: "exclamationmark.circle.fill")
                .font(.system(size: 11))
                .foregroundStyle(Color(red: 0.90, green: 0.22, blue: 0.21)) // #E53935
        case .received:
            EmptyView()
        }
    }
}

// MARK: - Standard Message Bubble (Matches Android MessageBubble: Responsive, Content-Hugging, Soft Lavender Container)
private struct MessageBubble: View {
    let message: MessageRecord
    let showSender: Bool

    private var isQuoted: Bool {
        message.text.hasPrefix("> [")
    }

    private var quoteHeader: (quote: String, body: String) {
        if let newline = message.text.firstIndex(of: "\n") {
            let q = String(message.text[..<newline])
            let b = String(message.text[message.text.index(after: newline)...])
            return (q.replacingOccurrences(of: "> ", with: ""), b)
        }
        return ("", message.text)
    }

    private var bubbleBg: Color {
        if message.outgoing {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.28, green: 0.20, blue: 0.45, alpha: 1.0)
                : UIColor(red: 0.92, green: 0.87, blue: 1.00, alpha: 1.0)
            })
        } else {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.18, green: 0.17, blue: 0.21, alpha: 1.0)
                : UIColor(red: 0.95, green: 0.93, blue: 0.97, alpha: 1.0)
            })
        }
    }

    private var textColor: Color {
        if message.outgoing {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.92, green: 0.87, blue: 1.0, alpha: 1.0)
                : UIColor(red: 0.11, green: 0.10, blue: 0.17, alpha: 1.0)
            })
        } else {
            return .primary
        }
    }

    private var subtextColor: Color {
        if message.outgoing {
            return Color(uiColor: UIColor { $0.userInterfaceStyle == .dark
                ? UIColor(red: 0.79, green: 0.77, blue: 0.82, alpha: 1.0)
                : UIColor(red: 0.40, green: 0.38, blue: 0.45, alpha: 1.0)
            })
        } else {
            return .secondary
        }
    }

    var body: some View {
        let mine = message.outgoing

        HStack(spacing: 0) {
            if mine {
                Spacer(minLength: 40)
            }

            VStack(alignment: .trailing, spacing: 3) {
                if showSender && !mine {
                    Text(message.fromName ?? NodeId(hex: message.fromNodeId)?.display ?? message.fromNodeId)
                        .font(.caption.bold())
                        .foregroundStyle(Color.accentColor)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                // Render Quoted Reply Box if present
                if isQuoted {
                    let parsed = quoteHeader
                    HStack(spacing: 6) {
                        RoundedRectangle(cornerRadius: 1.5)
                            .fill(Color.accentColor)
                            .frame(width: 3)
                        Text(parsed.quote)
                            .font(.caption2)
                            .foregroundStyle(mine ? textColor.opacity(0.85) : .secondary)
                            .lineLimit(2)
                    }
                    .padding(6)
                    .background(mine ? Color.white.opacity(0.35) : Color(.systemBackground), in: RoundedRectangle(cornerRadius: 6))

                    Text(parsed.body)
                        .font(.body)
                        .foregroundStyle(textColor)
                        .frame(alignment: .leading)
                } else {
                    Text(message.text)
                        .font(.body)
                        .foregroundStyle(textColor)
                        .frame(minWidth: 36, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                }

                // Bottom row: edited tag, timestamp, and status icon (hugging right edge)
                HStack(spacing: 4) {
                    if message.isEdited {
                        Text("edited ·")
                            .font(.system(size: 11).italic())
                            .foregroundStyle(subtextColor)
                    }
                    Text(message.timestamp, style: .time)
                        .font(.system(size: 11))
                        .foregroundStyle(subtextColor)

                    if mine {
                        DeliveryStatusIcon(status: message.status)
                    } else if !message.verified {
                        Text("unverified")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.red)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                bubbleBg,
                in: UnevenRoundedRectangle(
                    topLeadingRadius: 16,
                    bottomLeadingRadius: mine ? 16 : 4,
                    bottomTrailingRadius: mine ? 4 : 16,
                    topTrailingRadius: 16
                )
            )
            .frame(maxWidth: 300, alignment: mine ? .trailing : .leading)

            if !mine {
                Spacer(minLength: 40)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Swipe To Reply Container (Matches Android SwipeToReplyContainer)
struct SwipeToReplyContainer<Content: View>: View {
    let isOutgoing: Bool
    let onReply: () -> Void
    let content: () -> Content

    @State private var dragOffset: CGFloat = 0
    @State private var hasTriggeredHaptic: Bool = false

    private let maxDrag: CGFloat = 80
    private let triggerThreshold: CGFloat = 48

    init(isOutgoing: Bool, onReply: @escaping () -> Void, @ViewBuilder content: @escaping () -> Content) {
        self.isOutgoing = isOutgoing
        self.onReply = onReply
        self.content = content
    }

    var body: some View {
        ZStack(alignment: isOutgoing ? .trailing : .leading) {
            // Reply indicator icon behind the message bubble
            let progress = min(1.0, abs(dragOffset) / triggerThreshold)
            if progress > 0.05 {
                let isTriggered = abs(dragOffset) >= triggerThreshold
                ZStack {
                    Circle()
                        .fill(isTriggered ? Color.accentColor : Color(.secondarySystemBackground))
                        .frame(width: 36, height: 36)
                    Image(systemName: "arrowshape.turn.up.left.fill")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(isTriggered ? Color.white : Color.secondary)
                }
                .scaleEffect(0.6 + 0.4 * progress)
                .opacity(min(1.0, progress * 1.5))
                .padding(.horizontal, 16)
            }

            // Message Bubble with horizontal swipe translation
            content()
                .offset(x: dragOffset)
                .simultaneousGesture(
                    DragGesture(minimumDistance: 12, coordinateSpace: .local)
                        .onChanged { value in
                            let translation = value.translation.width
                            if isOutgoing {
                                // Slide left for sent messages
                                if translation < 0 {
                                    dragOffset = max(-maxDrag, translation)
                                    let triggered = abs(dragOffset) >= triggerThreshold
                                    if triggered && !hasTriggeredHaptic {
                                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                        hasTriggeredHaptic = true
                                    } else if !triggered && hasTriggeredHaptic {
                                        hasTriggeredHaptic = false
                                    }
                                }
                            } else {
                                // Slide right for received messages
                                if translation > 0 {
                                    dragOffset = min(maxDrag, translation)
                                    let triggered = abs(dragOffset) >= triggerThreshold
                                    if triggered && !hasTriggeredHaptic {
                                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                        hasTriggeredHaptic = true
                                    } else if !triggered && hasTriggeredHaptic {
                                        hasTriggeredHaptic = false
                                    }
                                }
                            }
                        }
                        .onEnded { _ in
                            let triggered = abs(dragOffset) >= triggerThreshold
                            if triggered {
                                UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                                onReply()
                            }
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.7)) {
                                dragOffset = 0
                            }
                            hasTriggeredHaptic = false
                        }
                )
        }
    }
}

// MARK: - Message Action Sheet (Matches Android MessageActionBottomSheet)
struct MessageActionSheet: View {
    let message: MessageRecord
    let onReply: () -> Void
    let onCopy: () -> Void
    let onEdit: () -> Void
    let onForward: () -> Void
    let onDelete: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 12) {
            // Snippet Preview
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(message.outgoing ? "You" : (message.fromName ?? "Peer"))
                        .font(.caption.bold())
                        .foregroundStyle(Color.accentColor)
                    Spacer()
                    Text(message.timestamp, style: .time)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Text(message.isVoice ? "🎤 Voice Note (\(max(1, (message.voiceDurationMs ?? 2000)/1000))s)" : message.text)
                    .font(.subheadline)
                    .foregroundStyle(.primary)
                    .lineLimit(2)
            }
            .padding(.horizontal, 16)
            .padding(.top, 14)
            .padding(.bottom, 8)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 16)

            Divider()
                .padding(.horizontal, 16)

            // Action Items
            VStack(spacing: 4) {
                actionRow(title: "Reply", icon: "arrowshape.turn.up.left.fill", color: Color.purple) {
                    dismiss()
                    onReply()
                }
                if !message.isVoice {
                    actionRow(title: "Copy Text", icon: "doc.on.doc.fill", color: Color.blue) {
                        dismiss()
                        onCopy()
                    }
                }
                if message.outgoing && !message.isVoice {
                    actionRow(title: "Edit Message", icon: "pencil", color: Color.orange) {
                        dismiss()
                        onEdit()
                    }
                }
                actionRow(title: "Forward", icon: "arrowshape.turn.up.right.fill", color: Color.teal) {
                    dismiss()
                    onForward()
                }
                actionRow(title: "Delete", icon: "trash.fill", color: Color.red, isDestructive: true) {
                    dismiss()
                    onDelete()
                }
            }
            .padding(.horizontal, 16)

            Spacer()
        }
        .padding(.top, 8)
    }

    private func actionRow(title: String, icon: String, color: Color, isDestructive: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    Circle()
                        .fill(color.opacity(0.15))
                        .frame(width: 36, height: 36)
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(color)
                }
                Text(title)
                    .font(.system(size: 15, weight: isDestructive ? .bold : .medium))
                    .foregroundStyle(isDestructive ? Color.red : Color.primary)
                Spacer()
            }
            .padding(.vertical, 6)
            .padding(.horizontal, 10)
            .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Forward Message Sheet (Matches Android ForwardBottomSheet)
struct ForwardMessageSheet: View {
    let message: MessageRecord
    let peers: [PeerRecord]
    let currentConversation: String
    let onForwardTo: (String, String) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section("Broadcast Channel") {
                    Button {
                        onForwardTo(Persistence.broadcastConversation, "Everyone nearby")
                        dismiss()
                    } label: {
                        HStack(spacing: 12) {
                            ZStack {
                                Circle().fill(Color.purple.opacity(0.15)).frame(width: 38, height: 38)
                                Image(systemName: "megaphone.fill").foregroundStyle(Color.purple)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Everyone nearby").font(.headline).foregroundStyle(.primary)
                                Text("Public mesh broadcast").font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "arrowshape.turn.up.right").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }

                if !peers.isEmpty {
                    Section("Direct Peer Conversations") {
                        ForEach(peers) { p in
                            Button {
                                onForwardTo(p.nodeId, p.name)
                                dismiss()
                            } label: {
                                HStack(spacing: 12) {
                                    AvatarView(nodeIdHex: p.nodeId, name: p.name)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(p.name).font(.headline).foregroundStyle(.primary)
                                        Text(NodeId(hex: p.nodeId)?.display ?? p.nodeId).font(.caption.monospaced()).foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Image(systemName: "arrowshape.turn.up.right").font(.caption).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Forward to...")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

