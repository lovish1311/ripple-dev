import SwiftUI
import SwiftData
import MapKit

struct EmergencySosHubView: View {
    @EnvironmentObject private var mesh: MeshService
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \SosRecord.timestamp, order: .reverse) private var allSosRecords: [SosRecord]
    @State private var filter: SosStatus = .active
    @State private var playingBeaconId: String? = nil
    @StateObject private var audioPlayer = OpusAudioPlayer()

    private var filteredRecords: [SosRecord] {
        allSosRecords.filter { $0.status == filter }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Status Filter Bar
                Picker("Status", selection: $filter) {
                    Text("Active (\(allSosRecords.filter { $0.status == .active }.count))").tag(SosStatus.active)
                    Text("Acknowledged (\(allSosRecords.filter { $0.status == .acknowledged }.count))").tag(SosStatus.acknowledged)
                    Text("Resolved (\(allSosRecords.filter { $0.status == .resolved }.count))").tag(SosStatus.resolved)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemBackground))

                if filteredRecords.isEmpty {
                    VStack(spacing: 14) {
                        Spacer()
                        Image(systemName: filter == .active ? "shield.checkmark.fill" : "archivebox.fill")
                            .font(.system(size: 48))
                            .foregroundStyle(filter == .active ? Color.green : Color.secondary)
                        Text(emptyStateTitle)
                            .font(.headline)
                            .foregroundStyle(.primary)
                        Text(emptyStateSubtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        Spacer()
                    }
                } else {
                    List {
                        ForEach(filteredRecords) { record in
                            SosIncidentCard(
                                record: record,
                                isPlaying: playingBeaconId == record.messageId && audioPlayer.isPlaying,
                                onPlayVoice: {
                                    handleToggleVoice(record)
                                },
                                onAcknowledge: {
                                    mesh.acknowledgeSos(beaconId: record.messageId)
                                },
                                onResolve: {
                                    mesh.resolveSos(beaconId: record.messageId)
                                },
                                onOpenThread: {
                                    audioPlayer.stop()
                                    dismiss()
                                    AppDelegate.openConversation?(Persistence.broadcastConversation)
                                },
                                onDelete: record.status != .active ? {
                                    mesh.container.mainContext.delete(record)
                                    try? mesh.container.mainContext.save()
                                } : nil
                            )
                            .listRowInsets(EdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14))
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Emergency SOS Hub")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        audioPlayer.stop()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
            .onDisappear {
                audioPlayer.stop()
            }
        }
    }

    private var emptyStateTitle: String {
        switch filter {
        case .active: return "No Active Emergencies"
        case .acknowledged: return "No Acknowledged Emergencies"
        case .resolved: return "No Resolved Incidents"
        }
    }

    private var emptyStateSubtitle: String {
        switch filter {
        case .active: return "All emergency beacons in the mesh network have been resolved or acknowledged."
        case .acknowledged: return "Beacons currently under response coordination will appear here."
        case .resolved: return "Cleared emergency incident history is stored here for post-incident debriefing."
        }
    }

    private func handleToggleVoice(_ record: SosRecord) {
        let sec = max(1, (record.voiceDurationMs ?? 4000) / 1000)
        if playingBeaconId == record.messageId && audioPlayer.isPlaying {
            audioPlayer.pause()
        } else if playingBeaconId == record.messageId && audioPlayer.currentTime > 0 {
            audioPlayer.resume()
        } else {
            playingBeaconId = record.messageId
            audioPlayer.play(data: record.voiceBytes, defaultDuration: Double(sec)) {
                playingBeaconId = nil
            }
        }
    }
}

// MARK: - SOS Incident Card Component
struct SosIncidentCard: View {
    let record: SosRecord
    let isPlaying: Bool
    let onPlayVoice: () -> Void
    let onAcknowledge: () -> Void
    let onResolve: () -> Void
    var onOpenThread: (() -> Void)? = nil
    var onDelete: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Header: Emergency Shield + Status Pills
            HStack(spacing: 8) {
                ZStack {
                    Circle().fill(Color.red).frame(width: 26, height: 26)
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                }
                Text("EMERGENCY SOS BEACON")
                    .font(.system(size: 13, weight: .heavy))
                    .foregroundStyle(Color.red)

                Spacer()

                if record.verified {
                    Text("VERIFIED")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(Color(red: 0.15, green: 0.55, blue: 0.3))
                        .padding(.horizontal, 7)
                        .padding(.vertical, 3)
                        .background(Color(red: 0.88, green: 0.96, blue: 0.90), in: Capsule())
                }

                statusPill
            }

            // Sender Details & Timestamp
            HStack {
                Text("From: \(record.fromName ?? "Ripple Peer") (\(String(record.fromNodeId.suffix(4))))")
                    .font(.subheadline.bold())
                    .foregroundStyle(Color.red.opacity(0.9))
                Spacer()
                Text(record.timestamp, style: .time)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            // Distress Note Body
            Text(record.text)
                .font(.subheadline)
                .foregroundStyle(.primary)
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 10))

            // Attached Voice Memo Player
            if record.hasVoice {
                HStack(spacing: 10) {
                    Button(action: onPlayVoice) {
                        ZStack {
                            Circle().fill(Color.red.opacity(0.12)).frame(width: 36, height: 36)
                            Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(Color.red)
                                .offset(x: isPlaying ? 0 : 1)
                        }
                    }
                    .buttonStyle(.plain)

                    VStack(alignment: .leading, spacing: 2) {
                        Text("🚨 Emergency Voice Memo")
                            .font(.caption.bold())
                            .foregroundStyle(Color.red)
                        let sec = max(1, (record.voiceDurationMs ?? 4000) / 1000)
                        Text(isPlaying ? "Playing voice memo..." : "0:0\(sec) · Opus 8kbps speech")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    // Waveform equalizer bars with live animated pulsing
                    HStack(spacing: 2.5) {
                        ForEach(0..<10) { i in
                            RoundedRectangle(cornerRadius: 1.25)
                                .fill(isPlaying ? Color.red : Color.red.opacity(0.3))
                                .frame(width: 2.5, height: isPlaying ? CGFloat([8, 16, 12, 20, 10, 15, 18, 11, 14, 9][i]) : CGFloat([6, 11, 8, 14, 7, 10, 12, 8, 10, 6][i]))
                                .animation(.easeInOut(duration: 0.15), value: isPlaying)
                        }
                    }
                    .frame(height: 22)
                }
                .padding(10)
                .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 10))
            }

            // Attached GPS Telemetry
            if let lat = record.latE7, let lng = record.lngE7 {
                let latD = Double(lat) / 1e7
                let lngD = Double(lng) / 1e7
                HStack {
                    Image(systemName: "location.fill")
                        .font(.caption)
                        .foregroundStyle(Color.red)
                    Text("\(String(format: "%.4f", latD))° N, \(String(format: "%.4f", lngD))° W (±\(record.accuracyMeters ?? 15)m)")
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.secondary)
                    Spacer()
                    Button {
                        openMaps(lat: latD, lng: lngD, name: record.fromName ?? "SOS Location")
                    } label: {
                        HStack(spacing: 4) {
                            Text("Open Map")
                            Image(systemName: "arrow.up.right.square")
                        }
                        .font(.caption.bold())
                        .foregroundStyle(Color.accentColor)
                    }
                }
                .padding(.horizontal, 4)
            }

            Divider().padding(.vertical, 2)

            // Action Buttons
            HStack(spacing: 8) {
                if record.status == .active {
                    if let onOpen = onOpenThread {
                        Button(action: onOpen) {
                            HStack(spacing: 4) {
                                Image(systemName: "bubble.left.fill")
                                Text("Open Thread")
                            }
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(Color.accentColor, in: RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                    }

                    Button(action: onAcknowledge) {
                        Label("ACK", systemImage: "checkmark.circle.fill")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(Color.green, in: RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)

                    Button(action: onResolve) {
                        Text("Resolve")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Color.secondary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 8)
                            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                } else if record.status == .acknowledged {
                    HStack {
                        Image(systemName: "checkmark.seal.fill")
                            .foregroundStyle(Color.green)
                        Text("ACK Delivered")
                            .font(.caption.bold())
                            .foregroundStyle(Color.green)
                    }
                    Spacer()
                    if let onOpen = onOpenThread {
                        Button(action: onOpen) {
                            Text("Open Thread")
                                .font(.caption.bold())
                                .foregroundStyle(Color.accentColor)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 6)
                                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 6))
                        }
                        .buttonStyle(.plain)
                    }
                    Button(action: onResolve) {
                        Label("Resolve", systemImage: "checkmark.circle")
                            .font(.caption.bold())
                            .foregroundStyle(Color.secondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 6))
                    }
                    .buttonStyle(.plain)
                    if let onDel = onDelete {
                        Button(action: onDel) {
                            Image(systemName: "trash")
                                .font(.caption)
                                .foregroundStyle(Color.red)
                                .padding(6)
                        }
                        .buttonStyle(.plain)
                    }
                } else {
                    HStack {
                        Image(systemName: "archivebox.fill")
                            .foregroundStyle(.secondary)
                        Text("Incident Resolved")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    if let onOpen = onOpenThread {
                        Button(action: onOpen) {
                            Text("Open Thread")
                                .font(.caption.bold())
                                .foregroundStyle(Color.accentColor)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 6)
                                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 6))
                        }
                        .buttonStyle(.plain)
                    }
                    if let onDel = onDelete {
                        Button(action: onDel) {
                            Image(systemName: "trash")
                                .font(.caption)
                                .foregroundStyle(Color.red)
                                .padding(6)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(14)
        .background(Color.red.opacity(0.06))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color.red.opacity(0.35), lineWidth: 1.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var statusPill: some View {
        Group {
            switch record.status {
            case .active:
                Text("ACTIVE")
                    .font(.system(size: 9, weight: .black))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.red, in: Capsule())
            case .acknowledged:
                Text("ACKNOWLEDGED")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(Color.green)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.green.opacity(0.15), in: Capsule())
            case .resolved:
                Text("RESOLVED")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(Color.secondary)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.secondary.opacity(0.15), in: Capsule())
            }
        }
    }

    private func openMaps(lat: Double, lng: Double, name: String) {
        let coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lng)
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: coordinate))
        mapItem.name = "SOS: \(name)"
        mapItem.openInMaps(launchOptions: [
            MKLaunchOptionsMapTypeKey: MKMapType.standard.rawValue
        ])
    }
}
