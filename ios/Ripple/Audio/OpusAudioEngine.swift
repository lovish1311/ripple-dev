import Foundation
import AVFoundation
import Combine

/// Production-grade audio engine for ultra-low bitrate offline mesh voice notes on iOS.
///
/// Uses Apple CoreAudio's native `kAudioFormatOpus` inside a CAF container:
/// 16 kHz, 16-bit PCM mono input, encoded at 8000 bps (8 kbps).
/// A 4-second emergency voice memo is ~3.5 KB to 4.5 KB, fitting cleanly within
/// a single BLE packet frame without fragmentation issues.
final class OpusAudioRecorder: NSObject, ObservableObject, AVAudioRecorderDelegate {
    static let shared = OpusAudioRecorder()

    @Published private(set) var isRecording = false
    @Published private(set) var duration: Double = 0.0
    @Published private(set) var amplitude: Float = 0.0

    private var audioRecorder: AVAudioRecorder?
    private var timer: Timer?
    private var currentFileUrl: URL?
    private var completionHandler: ((Int, Data) -> Void)?

    static let sampleRate: Double = 16000.0
    static let bitRate: Int = 8000
    static let maxDuration: Double = 4.0

    override init() {
        super.init()
    }

    /// Request microphone permission and start recording a voice memo.
    func startRecording(onAutoStop: ((Int, Data) -> Void)? = nil) -> Bool {
        guard !isRecording else { return false }
        completionHandler = onAutoStop

        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .voiceChat, options: [.defaultToSpeaker, .allowBluetooth])
            try session.setActive(true)
        } catch {
            print("Failed to set audio session category: \(error)")
            return false
        }

        let tempDir = FileManager.default.temporaryDirectory
        let fileUrl = tempDir.appendingPathComponent("memo_\(UUID().uuidString).caf")
        currentFileUrl = fileUrl

        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatOpus),
            AVSampleRateKey: Self.sampleRate,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: Self.bitRate,
            AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue
        ]

        do {
            let recorder = try AVAudioRecorder(url: fileUrl, settings: settings)
            recorder.delegate = self
            recorder.isMeteringEnabled = true
            guard recorder.record() else {
                return false
            }
            audioRecorder = recorder
            isRecording = true
            duration = 0.0
            amplitude = 0.0

            timer?.invalidate()
            timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
                guard let self = self, let r = self.audioRecorder, r.isRecording else { return }
                r.updateMeters()
                let power = r.averagePower(forChannel: 0)
                // Normalize dB (-60 to 0) to 0.0 ... 1.0
                let normalized = max(0.0, min(1.0, (power + 60.0) / 60.0))
                self.amplitude = normalized
                self.duration = r.currentTime

                if self.duration >= Self.maxDuration {
                    _ = self.stopRecording()
                }
            }
            return true
        } catch {
            print("Failed to initialize AVAudioRecorder with Opus: \(error)")
            return false
        }
    }

    /// Stop recording and return the encoded audio data and duration in ms.
    func stopRecording() -> (durationMs: Int, data: Data)? {
        guard isRecording else { return nil }
        timer?.invalidate()
        timer = nil

        let durMs = Int(duration * 1000)
        audioRecorder?.stop()
        audioRecorder = nil
        isRecording = false
        amplitude = 0.0

        defer {
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        }

        guard let url = currentFileUrl, let data = try? Data(contentsOf: url) else {
            return nil
        }
        try? FileManager.default.removeItem(at: url)
        currentFileUrl = nil

        completionHandler?(durMs, data)
        completionHandler = nil
        return (durMs, data)
    }

    func cancelRecording() {
        timer?.invalidate()
        timer = nil
        audioRecorder?.stop()
        audioRecorder = nil
        isRecording = false
        amplitude = 0.0

        if let url = currentFileUrl {
            try? FileManager.default.removeItem(at: url)
            currentFileUrl = nil
        }
        completionHandler = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}

/// Standalone audio player for Opus CAF audio payloads.
final class OpusAudioPlayer: NSObject, ObservableObject, AVAudioPlayerDelegate {
    @Published private(set) var isPlaying = false
    @Published private(set) var currentTime: Double = 0.0
    @Published private(set) var duration: Double = 0.0

    private var player: AVAudioPlayer?
    private var timer: Timer?
    private var onFinished: (() -> Void)?

    func play(data: Data, onFinished: (() -> Void)? = nil) {
        stop()
        self.onFinished = onFinished

        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .default, options: [.duckOthers, .defaultToSpeaker])
            try session.setActive(true)
        } catch {
            print("Audio session setup failed: \(error)")
        }

        do {
            let p = try AVAudioPlayer(data: data)
            p.delegate = self
            p.prepareToPlay()
            p.play()
            player = p
            duration = p.duration
            isPlaying = true

            timer?.invalidate()
            timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
                guard let self = self, let p = self.player, p.isPlaying else { return }
                self.currentTime = p.currentTime
            }
        } catch {
            print("AVAudioPlayer failed to play Opus data: \(error)")
            self.onFinished?()
            self.onFinished = nil
        }
    }

    func pause() {
        player?.pause()
        isPlaying = false
        timer?.invalidate()
        timer = nil
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        player?.stop()
        player = nil
        isPlaying = false
        currentTime = 0.0
        onFinished?()
        onFinished = nil
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        stop()
    }
}
