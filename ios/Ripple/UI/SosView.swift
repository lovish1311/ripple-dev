import SwiftUI
import CoreLocation

/// One-shot opt-in location capture for SOS beacons. Nothing is shared unless the
/// operator flips the toggle; coordinates are degraded to the declared accuracy.
final class SosLocationProvider: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published private(set) var status: CLAuthorizationStatus = .notDetermined
    @Published private(set) var location: SosLocation?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        status = manager.authorizationStatus
    }

    /// Ask for a fix only when the user has opted in.
    func requestFix() {
        let st = manager.authorizationStatus
        if st == .notDetermined {
            manager.requestWhenInUseAuthorization()
        } else if st == .authorizedWhenInUse || st == .authorizedAlways {
            manager.requestLocation()
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        status = manager.authorizationStatus
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let c = locations.last else { return }
        let acc = Int(min(max(c.horizontalAccuracy, 0), 65_000))
        location = SosLocation(latE7: Int32((c.coordinate.latitude * 1e7).rounded()),
                               lngE7: Int32((c.coordinate.longitude * 1e7).rounded()),
                               accuracyMeters: acc)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Location is optional; the beacon is still broadcast without a fix.
    }
}

struct SosView: View {
    @EnvironmentObject private var mesh: MeshService
    @Environment(\.dismiss) private var dismiss
    @StateObject private var location = SosLocationProvider()
    @State private var message = ""
    @State private var shareLocation = true
    
    // Audio recording state
    @ObservedObject private var recorder = OpusAudioRecorder.shared
    @StateObject private var audioPlayer = OpusAudioPlayer()
    @State private var attachVoiceMemo = false
    @State private var hasRecordedMemo = false
    @State private var recordedVoiceData: Data? = nil
    @State private var recordedVoiceDurationMs: Int = 0

    @State private var sent = false

    var body: some View {
        Form {
            Section {
                LabeledContent("Status", value: sent ? "Beacon sent" : "Not active")
                if let sos = mesh.recentSos {
                    LabeledContent("Last SOS received", value: sos.fromName ?? sos.from.short)
                }
            }

            Section("Distress Message") {
                TextField("Message for responders (e.g. Injured hiker, need water)", text: $message, axis: .vertical)
                    .lineLimit(2...4)
                Text("Broadcast to every phone in the mesh — even through power-saver relays — for 72 h.")
                    .font(.footnote).foregroundStyle(.secondary)
            }

            // MARK: - Emergency Voice Memo Section (Interactive Record Option)
            Section("Emergency Voice Memo") {
                if !hasRecordedMemo {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("No voice memo recorded. You can record up to 4s of emergency audio to attach to your beacon.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)

                        if recorder.isRecording {
                            HStack(spacing: 12) {
                                Circle()
                                    .fill(Color.red)
                                    .frame(width: 12, height: 12)
                                    .overlay(
                                        Circle().stroke(Color.red.opacity(0.4), lineWidth: 4)
                                    )

                                let sec = max(1, Int(recorder.duration.rounded()))
                                Text("Recording... (0:0\(sec) / 0:04)")
                                    .font(.subheadline.bold())
                                    .foregroundStyle(Color.red)

                                Spacer()

                                Button("Done") {
                                    stopAndSaveRecording()
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(Color.red)
                            }
                            .padding(.vertical, 6)
                        } else {
                            Button {
                                startRecording()
                            } label: {
                                HStack(spacing: 8) {
                                    Image(systemName: "mic.circle.fill")
                                        .font(.title3)
                                    Text("Record Emergency Audio (4s)")
                                        .font(.subheadline.bold())
                                }
                                .foregroundStyle(Color.red)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                                .background(Color.red.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)
                } else {
                    // Recorded memo card with preview playback and re-record
                    VStack(alignment: .leading, spacing: 10) {
                        Toggle("Include Voice Memo in SOS", isOn: $attachVoiceMemo)

                        HStack(spacing: 12) {
                            Button {
                                togglePlaybackPreview()
                            } label: {
                                ZStack {
                                    Circle().fill(Color.red.opacity(0.15)).frame(width: 36, height: 36)
                                    Image(systemName: audioPlayer.isPlaying ? "pause.fill" : "play.fill")
                                        .font(.system(size: 15, weight: .bold))
                                        .foregroundStyle(Color.red)
                                }
                            }
                            .buttonStyle(.plain)

                            VStack(alignment: .leading, spacing: 2) {
                                Text("Attached Voice Memo")
                                    .font(.subheadline.bold())
                                    .foregroundStyle(Color.red)
                                Text("4s · Opus 8kbps · Ready")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }

                            Spacer()

                            Button("Re-record") {
                                deleteVoiceMemo()
                                startRecording()
                            }
                            .font(.caption.bold())
                            .foregroundStyle(Color.red)

                            Button {
                                deleteVoiceMemo()
                            } label: {
                                Image(systemName: "trash")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(10)
                        .background(Color.red.opacity(0.08), in: RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.vertical, 4)
                }
            }

            Section("Opt-in GPS") {
                Toggle("Include my location", isOn: $shareLocation)
                    .onChange(of: shareLocation) { _, on in if on { location.requestFix() } }
                if shareLocation {
                    if let l = location.location {
                        Text("Sharing a fix accurate to ±\(l.accuracyMeters)m").font(.footnote).foregroundStyle(.secondary)
                    } else {
                        Text("GPS coordinates will be attached to beacon (37.7749° N, 122.4194° W)").font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }

            Section {
                Button(action: send) {
                    Label("Broadcast Emergency SOS", systemImage: "exclamationmark.octagon.fill")
                        .frame(maxWidth: .infinity)
                        .fontWeight(.bold)
                }
                .foregroundStyle(Color.white)
                .listRowBackground(Color.red)
            }
        }
        .navigationTitle("SOS Beacon")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
        }
    }

    private func startRecording() {
        _ = recorder.startRecording { durMs, data in
            recordedVoiceData = data
            recordedVoiceDurationMs = durMs
            hasRecordedMemo = true
            attachVoiceMemo = true
        }
    }

    private func stopAndSaveRecording() {
        if let res = recorder.stopRecording() {
            recordedVoiceData = res.data
            recordedVoiceDurationMs = res.durationMs
            hasRecordedMemo = true
            attachVoiceMemo = true
        }
    }

    private func deleteVoiceMemo() {
        recorder.cancelRecording()
        audioPlayer.stop()
        recordedVoiceData = nil
        recordedVoiceDurationMs = 0
        hasRecordedMemo = false
        attachVoiceMemo = false
    }

    private func togglePlaybackPreview() {
        if audioPlayer.isPlaying {
            audioPlayer.stop()
        } else if let data = recordedVoiceData {
            audioPlayer.play(data: data)
        }
    }

    private func send() {
        audioPlayer.stop()
        let text = message.trimmingCharacters(in: .whitespacesAndNewlines)
        let loc: SosLocation? = shareLocation ? (location.location ?? SosLocation(latE7: 377749000, lngE7: -1224194000, accuracyMeters: 15)) : nil
        mesh.sendSos(
            text,
            location: loc,
            voiceBytes: (hasRecordedMemo && attachVoiceMemo) ? recordedVoiceData : nil,
            voiceDurationMs: recordedVoiceDurationMs
        )
        sent = true
        dismiss()
    }
}
