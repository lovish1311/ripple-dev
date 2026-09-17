import SwiftUI
import SwiftData
import MapKit

struct HomeView: View {
    @EnvironmentObject private var mesh: MeshService
    @ObservedObject private var appearance = AppearanceSettings.shared
    @Query(sort: \MessageRecord.timestamp, order: .reverse) private var messages: [MessageRecord]
    @Query(sort: \PeerRecord.lastSeen, order: .reverse) private var peers: [PeerRecord]
    @Query(sort: \SosRecord.timestamp, order: .reverse) private var sosRecords: [SosRecord]
    @State private var path = NavigationPath()
    @State private var showSettings = false
    @State private var showSos = false
    @State private var showSosHub = false
    @State private var tab = 0

    private var activeBeacons: [SosRecord] {
        sosRecords.filter { $0.status == .active }
    }

    private struct Summary: Identifiable {
        let conversation: String; let lastText: String; let lastTimestamp: Date; let unread: Int
        var id: String { conversation }
    }

    private var summaries: [Summary] {
        var seen = Set<String>(); var out: [Summary] = []
        for m in messages where !seen.contains(m.conversation) {
            seen.insert(m.conversation)
            let unread = messages.filter { $0.conversation == m.conversation && !$0.outgoing && $0.status == .received }.count
            out.append(Summary(conversation: m.conversation, lastText: m.text, lastTimestamp: m.timestamp, unread: unread))
        }
        return out
    }

    var body: some View {
        NavigationStack(path: $path) {
            ZStack(alignment: .bottomTrailing) {
                VStack(spacing: 0) {
                    Picker("", selection: $tab) {
                        Text("Chats").tag(0)
                        Text("Peers (\(peers.count))").tag(1)
                    }
                    .pickerStyle(.segmented).padding(.horizontal).padding(.bottom, 8)

                    if tab == 0 { chatList } else { peerList }
                }

                // SOS Circular Floating Action Button (Exact Android Circle with bold SOS)
                Button {
                    showSos = true
                } label: {
                    Text("SOS")
                        .font(.system(size: 18, weight: .black))
                        .tracking(1.0)
                        .foregroundStyle(.white)
                        .frame(width: 58, height: 58)
                        .background(Color.red, in: Circle())
                        .shadow(color: Color.red.opacity(0.45), radius: 6, x: 0, y: 4)
                }
                .padding(.trailing, 16)
                .padding(.bottom, 16)
            }
            .navigationTitle("Ripple")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { showSosHub = true } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(.red)
                            if !activeBeacons.isEmpty {
                                Text("\(activeBeacons.count)")
                                    .font(.caption2.bold())
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(Color.red, in: Capsule())
                            }
                        }
                    }
                    .accessibilityLabel("Emergency SOS Hub")
                }
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 0) {
                        Text("Ripple").font(.headline)
                        Text(statusLine).font(.caption2).foregroundStyle(mesh.status.bluetoothOn ? Color.accentColor : .red)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        .accessibilityLabel("Settings")
                }
            }
            .navigationDestination(for: String.self) { target in
                switch target {
                case "appearance":
                    AppearanceView()
                case "profile":
                    ProfileView()
                case "settings":
                    SettingsView()
                default:
                    ChatView(conversation: target)
                }
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
                    .environmentObject(mesh)
                    .preferredColorScheme(appearance.colorScheme)
                    .tint(appearance.currentTheme.primaryColor)
            }
            .sheet(isPresented: $showSos) {
                NavigationStack {
                    SosView()
                }
                .preferredColorScheme(appearance.colorScheme)
                .tint(appearance.currentTheme.primaryColor)
            }
            .sheet(isPresented: $showSosHub) {
                EmergencySosHubView()
                    .environmentObject(mesh)
                    .preferredColorScheme(appearance.colorScheme)
                    .tint(appearance.currentTheme.primaryColor)
            }
            .onAppear {
                AppDelegate.openConversation = { path.append($0) }
                seedSampleDataIfNeeded()
            }
        }
    }

    private func seedSampleDataIfNeeded() {
        let ctx = mesh.container.mainContext
        if sosRecords.isEmpty {
            let seedSos = SosRecord(
                messageId: "seed-sos-1",
                fromNodeId: "node-asha-2002",
                fromName: "Asha",
                text: "Injured hiker with severe ankle sprain near North Trail marker 4. Need first aid kit & water.",
                latE7: Int32(37.7749 * 1e7),
                lngE7: Int32(-122.4194 * 1e7),
                accuracyMeters: 15,
                verified: true,
                timestamp: Date().addingTimeInterval(-900),
                status: .active,
                voiceBytes: nil,
                voiceDurationMs: 4000
            )
            ctx.insert(seedSos)
            try? ctx.save()
        }
    }

    private var statusLine: String {
        guard mesh.status.bluetoothOn else { return "Bluetooth is off" }
        let s = mesh.status
        return "\(s.directLinks) direct link\(s.directLinks == 1 ? "" : "s") · \(s.knownPeers) peers known"
    }

    private var chatList: some View {
        List {
            // Master SOS Banner if multiple active emergencies exist
            if activeBeacons.count >= 2 {
                Section {
                    Button {
                        showSosHub = true
                    } label: {
                        HStack(spacing: 10) {
                            ZStack {
                                Circle().fill(Color.red).frame(width: 26, height: 26)
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                            Text("🚨 \(activeBeacons.count) Active Emergencies")
                                .font(.system(size: 14, weight: .heavy))
                                .foregroundStyle(Color.red)
                            Spacer()
                            Text("View All (\(activeBeacons.count))")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(Color.red)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 5)
                                .background(Color.red.opacity(0.12), in: Capsule())
                        }
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.plain)
                }
                .listRowBackground(Color.red.opacity(0.08))
            } else if let singleActive = activeBeacons.first {
                // Priority Pinned Single SOS Emergency Card
                Section {
                    PinnedHomeSosCard(
                        record: singleActive,
                        onOpenThread: {
                            path.append(Persistence.broadcastConversation)
                        },
                        onAcknowledge: {
                            mesh.acknowledgeSos(beaconId: singleActive.messageId)
                        }
                    )
                }
                .listRowInsets(EdgeInsets(top: 6, leading: 12, bottom: 6, trailing: 12))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            }

            let broadcast = summaries.first { $0.conversation == Persistence.broadcastConversation }
            NavigationLink(value: Persistence.broadcastConversation) {
                HStack(spacing: 12) {
                    Image(systemName: "megaphone.fill").font(.title2).foregroundStyle(Color.accentColor).frame(width: 40)
                    VStack(alignment: .leading) {
                        Text("Everyone nearby").font(.headline)
                        Text(broadcast?.lastText ?? "Public channel — reaches every phone in the mesh").font(.subheadline).foregroundStyle(.secondary).lineLimit(1)
                    }
                    Spacer()
                    if let u = broadcast?.unread, u > 0 { UnreadBadge(count: u) }
                }
            }
            ForEach(summaries.filter { $0.conversation != Persistence.broadcastConversation }) { s in
                NavigationLink(value: s.conversation) {
                    HStack(spacing: 12) {
                        AvatarView(nodeIdHex: s.conversation)
                        VStack(alignment: .leading) {
                            Text(peers.first { $0.nodeId == s.conversation }?.name ?? NodeId(hex: s.conversation)?.display ?? s.conversation).font(.headline)
                            Text(s.lastText).font(.subheadline).foregroundStyle(.secondary).lineLimit(1)
                        }
                        Spacer()
                        VStack(alignment: .trailing) {
                            Text(s.lastTimestamp, style: .time).font(.caption2).foregroundStyle(.secondary)
                            if s.unread > 0 { UnreadBadge(count: s.unread) }
                        }
                    }
                }
            }
        }
        .listStyle(.plain)
    }

    private var peerList: some View {
        Group {
            if peers.isEmpty {
                ContentUnavailableView {
                    Label("No peers yet", systemImage: "dot.radiowaves.left.and.right")
                } description: {
                    Text("Keep Bluetooth on and bring another phone running Ripple within range.")
                } actions: {
                    Button("Only one phone? Try the simulated neighbourhood") { showSettings = true }
                }
            } else {
                List(peers) { p in
                    let online = Date().timeIntervalSince(p.lastSeen) < 300
                    NavigationLink(value: p.nodeId) {
                        HStack(spacing: 12) {
                            AvatarView(nodeIdHex: p.nodeId)
                            VStack(alignment: .leading) {
                                Text(p.name).font(.headline)
                                HStack(spacing: 6) {
                                    Text(NodeId(hex: p.nodeId)?.display ?? p.nodeId).font(.caption.monospaced())
                                    Text("· \(p.hops) hop\(p.hops == 1 ? "" : "s")").font(.caption)
                                }.foregroundStyle(.secondary)
                            }
                            Spacer()
                            Circle().fill(online ? Color.green : Color.gray.opacity(0.4)).frame(width: 10, height: 10)
                                .accessibilityLabel(online ? "Online" : "Offline")
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
    }
}

struct UnreadBadge: View {
    let count: Int
    var body: some View {
        Text("\(count)").font(.caption2.bold()).foregroundStyle(.white)
            .padding(.horizontal, 7).padding(.vertical, 3).background(Color.accentColor, in: Capsule())
            .accessibilityLabel("\(count) unread message\(count == 1 ? "" : "s")")
    }
}

/// Deterministic coloured circle derived from the node id.
struct AvatarView: View {
    let nodeIdHex: String
    var body: some View {
        let n = Int(nodeIdHex.prefix(6), radix: 16) ?? 0
        let hue = Double(n % 360) / 360.0
        ZStack {
            Circle().fill(Color(hue: hue, saturation: 0.45, brightness: 0.75))
            Text(nodeIdHex.suffix(2).uppercased()).font(.subheadline.bold()).foregroundStyle(.white)
        }
        .frame(width: 40, height: 40)
    }
}

// MARK: - Pinned Single SOS Emergency Card on Home Screen
struct PinnedHomeSosCard: View {
    let record: SosRecord
    let onOpenThread: () -> Void
    let onAcknowledge: () -> Void

    @StateObject private var audioPlayer = OpusAudioPlayer()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Header
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

                Spacer()

                if record.verified {
                    Text("VERIFIED")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(Color(red: 0.15, green: 0.55, blue: 0.3))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color(red: 0.88, green: 0.96, blue: 0.90), in: Capsule())
                }

                Text(record.timestamp, style: .time)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Sender
            Text("From: \(record.fromName ?? "Ripple Node") (\(String(record.fromNodeId.suffix(4))))")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Color.red.opacity(0.85))

            // Message text
            Text(record.text)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.primary)
                .padding(8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 8))

            // Attached Voice Memo
            if let voice = record.voiceBytes, !voice.isEmpty {
                HStack(spacing: 10) {
                    Button {
                        if audioPlayer.isPlaying {
                            audioPlayer.stop()
                        } else {
                            audioPlayer.play(data: voice)
                        }
                    } label: {
                        ZStack {
                            Circle().fill(Color.red.opacity(0.15)).frame(width: 32, height: 32)
                            Image(systemName: audioPlayer.isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(Color.red)
                        }
                    }
                    .buttonStyle(.plain)

                    VStack(alignment: .leading, spacing: 1) {
                        Text("Emergency Voice Memo")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Color.red)
                        let sec = max(1, (record.voiceDurationMs ?? 4000) / 1000)
                        Text(audioPlayer.isPlaying ? "Playing voice memo..." : "0:0\(sec) · Opus 8kbps audio")
                            .font(.system(size: 10))
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    HStack(spacing: 2) {
                        ForEach(0..<6) { i in
                            RoundedRectangle(cornerRadius: 1)
                                .fill(audioPlayer.isPlaying ? Color.red : Color.red.opacity(0.35))
                                .frame(width: 2.5, height: audioPlayer.isPlaying ? CGFloat([8, 14, 11, 17, 9, 13][i]) : 7)
                                .animation(.easeInOut(duration: 0.2).repeatForever().delay(Double(i) * 0.05), value: audioPlayer.isPlaying)
                        }
                    }
                    .frame(height: 18)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 8))
            }

            // Attached GPS
            if let lat = record.latE7, let lng = record.lngE7 {
                let latD = Double(lat) / 1e7
                let lngD = Double(lng) / 1e7
                HStack {
                    Image(systemName: "location.fill")
                        .font(.caption2)
                        .foregroundStyle(Color.red)
                    Text("\(String(format: "%.4f", latD))° N, \(String(format: "%.4f", lngD))° W")
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button {
                        let coordinate = CLLocationCoordinate2D(latitude: latD, longitude: lngD)
                        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: coordinate))
                        mapItem.name = "SOS Location"
                        mapItem.openInMaps()
                    } label: {
                        Text("Open Map")
                            .font(.caption2.bold())
                            .foregroundStyle(Color.accentColor)
                    }
                }
                .padding(.horizontal, 4)
            }

            Divider().padding(.vertical, 1)

            // Primary actions
            HStack(spacing: 8) {
                Button(action: onOpenThread) {
                    Label("Open Thread", systemImage: "bubble.left.and.bubble.right.fill")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 7)
                        .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)

                Button(action: onAcknowledge) {
                    Label("Acknowledge (ACK)", systemImage: "checkmark.circle.fill")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 7)
                        .background(Color.green, in: RoundedRectangle(cornerRadius: 8))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .background(Color.red.opacity(0.07))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.red.opacity(0.4), lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .onDisappear {
            audioPlayer.stop()
        }
    }
}
