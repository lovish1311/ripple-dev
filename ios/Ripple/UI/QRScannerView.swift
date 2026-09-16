import SwiftUI
import AVFoundation

/// AVFoundation-powered QR code scanner for out-of-band peer pairing (ROADMAP Phase 0.2).
///
/// Features:
/// - Real-time QR frame detection via `AVCaptureMetadataOutput`
/// - Semi-transparent mask overlay with rounded focus square and scanline animation
/// - Torch / Flashlight toggle
/// - Graceful camera authorization handling & simulator fallback
/// - Haptic feedback on successful detection
struct QRScannerSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onScanned: (String) -> Void

    @State private var isTorchOn = false
    @State private var cameraError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                if let error = cameraError {
                    VStack(spacing: 16) {
                        Image(systemName: "camera.trianglebadge.exclamationmark")
                            .font(.system(size: 48))
                            .foregroundStyle(.secondary)
                        Text(error)
                            .font(.headline)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        if error.contains("permission") {
                            Button("Open Settings") {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    }
                    .padding()
                } else {
                    QRScannerRepresentable(
                        isTorchOn: isTorchOn,
                        onError: { err in cameraError = err },
                        onScanned: { code in
                            let generator = UINotificationFeedbackGenerator()
                            generator.notificationOccurred(.success)
                            onScanned(code)
                            dismiss()
                        }
                    )
                    .ignoresSafeArea()

                    // Viewfinder Reticle Overlay
                    ScannerOverlayView()
                }
            }
            .navigationTitle("Scan Peer QR Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(.white)
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        isTorchOn.toggle()
                    } label: {
                        Image(systemName: isTorchOn ? "bolt.fill" : "bolt.slash.fill")
                            .foregroundStyle(.white)
                    }
                    .disabled(cameraError != nil)
                }
            }
            .toolbarBackground(.black, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
    }
}

private struct QRScannerRepresentable: UIViewControllerRepresentable {
    let isTorchOn: Bool
    let onError: (String) -> Void
    let onScanned: (String) -> Void

    func makeUIViewController(context: Context) -> QRScannerViewController {
        let vc = QRScannerViewController()
        vc.delegate = context.coordinator
        return vc
    }

    func updateUIViewController(_ uiViewController: QRScannerViewController, context: Context) {
        uiViewController.setTorch(isTorchOn)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onError: onError, onScanned: onScanned)
    }

    final class Coordinator: NSObject, QRScannerViewControllerDelegate {
        let onError: (String) -> Void
        let onScanned: (String) -> Void

        init(onError: @escaping (String) -> Void, onScanned: @escaping (String) -> Void) {
            self.onError = onError
            self.onScanned = onScanned
        }

        func qrScannerDidFail(with error: String) {
            DispatchQueue.main.async { self.onError(error) }
        }

        func qrScannerDidScan(code: String) {
            DispatchQueue.main.async { self.onScanned(code) }
        }
    }
}

protocol QRScannerViewControllerDelegate: AnyObject {
    func qrScannerDidFail(with error: String)
    func qrScannerDidScan(code: String)
}

final class QRScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    weak var delegate: QRScannerViewControllerDelegate?

    private let captureSession = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var isScanning = true

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        checkPermissionsAndSetup()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        isScanning = true
        if !captureSession.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.captureSession.startRunning()
            }
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if captureSession.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.captureSession.stopRunning()
            }
        }
    }

    private func checkPermissionsAndSetup() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            setupCamera()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted {
                        self?.setupCamera()
                    } else {
                        self?.delegate?.qrScannerDidFail(with: "Camera permission is required to scan peer QR codes.")
                    }
                }
            }
        case .denied, .restricted:
            delegate?.qrScannerDidFail(with: "Camera permission is disabled. Please enable it in Settings.")
        @unknown default:
            delegate?.qrScannerDidFail(with: "Camera access unavailable.")
        }
    }

    private func setupCamera() {
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            delegate?.qrScannerDidFail(with: "No camera device found on this hardware (e.g. iOS Simulator).")
            return
        }

        let videoInput: AVCaptureDeviceInput
        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            delegate?.qrScannerDidFail(with: "Unable to initialize camera input.")
            return
        }

        captureSession.beginConfiguration()

        if captureSession.canAddInput(videoInput) {
            captureSession.addInput(videoInput)
        } else {
            captureSession.commitConfiguration()
            delegate?.qrScannerDidFail(with: "Cannot add video input.")
            return
        }

        let metadataOutput = AVCaptureMetadataOutput()
        if captureSession.canAddOutput(metadataOutput) {
            captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            captureSession.commitConfiguration()
            delegate?.qrScannerDidFail(with: "Cannot configure QR detection output.")
            return
        }

        captureSession.commitConfiguration()

        let preview = AVCaptureVideoPreviewLayer(session: captureSession)
        preview.frame = view.bounds
        preview.videoGravity = .resizeAspectFill
        view.layer.addSublayer(preview)
        self.previewLayer = preview

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            self?.captureSession.startRunning()
        }
    }

    func setTorch(_ on: Bool) {
        guard let device = AVCaptureDevice.default(for: .video), device.hasTorch else { return }
        do {
            try device.lockForConfiguration()
            device.torchMode = on ? .on : .off
            device.unlockForConfiguration()
        } catch {
            // Ignore torch failures gracefully
        }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard isScanning, let metadataObject = metadataObjects.first,
              let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject,
              let stringValue = readableObject.stringValue else { return }

        isScanning = false
        captureSession.stopRunning()
        delegate?.qrScannerDidScan(code: stringValue)
    }
}

/// Stylized viewfinder with animated laser line and corner reticle
private struct ScannerOverlayView: View {
    @State private var scanlineOffset: CGFloat = -110

    var body: some View {
        GeometryReader { geometry in
            let size: CGFloat = min(geometry.size.width * 0.75, 260)

            ZStack {
                // Semi-transparent cutout mask
                Color.black.opacity(0.5)
                    .mask(
                        Rectangle()
                            .fill(style: FillStyle(eoFill: true))
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .frame(width: size, height: size)
                                    .blendMode(.destinationOut)
                            )
                    )

                // Corner Brackets
                RoundedRectangle(cornerRadius: 16)
                    .strokeBorder(Color.white.opacity(0.3), lineWidth: 1)
                    .frame(width: size, height: size)

                CornerReticle()
                    .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
                    .frame(width: size, height: size)

                // Animated Scan Laser Line
                Rectangle()
                    .fill(
                        LinearGradient(
                            colors: [.clear, Color.accentColor.opacity(0.8), .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: size - 20, height: 2)
                    .offset(y: scanlineOffset)
                    .onAppear {
                        withAnimation(
                            .easeInOut(duration: 2.0)
                            .repeatForever(autoreverses: true)
                        ) {
                            scanlineOffset = 110
                        }
                    }

                // Instruction label
                VStack {
                    Spacer()
                    Text("Align the QR code within the frame")
                        .font(.subheadline)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(.ultraThinMaterial, in: Capsule())
                        .padding(.bottom, 60)
                }
            }
        }
    }
}

private struct CornerReticle: Shape {
    let cornerLength: CGFloat = 24

    func path(in rect: CGRect) -> Path {
        var path = Path()

        // Top-Left
        path.move(to: CGPoint(x: rect.minX, y: rect.minY + cornerLength))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX + cornerLength, y: rect.minY))

        // Top-Right
        path.move(to: CGPoint(x: rect.maxX - cornerLength, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + cornerLength))

        // Bottom-Right
        path.move(to: CGPoint(x: rect.maxX, y: rect.maxY - cornerLength))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX - cornerLength, y: rect.maxY))

        // Bottom-Left
        path.move(to: CGPoint(x: rect.minX + cornerLength, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY - cornerLength))

        return path
    }
}
