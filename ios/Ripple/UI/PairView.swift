import SwiftUI
import UIKit

/// Settings → Pair & verify (ROADMAP Phase 0.2; see docs/PAIRING.md).
///
/// Shows this device's identity code as a QR (Core Image), lets it be copied/shared as text,
/// imports a peer's code by live camera scan or paste with a parsed summary + 12-digit safety code,
/// and lists the persisted verified peers. A same-id/different-key import is refused via the
/// shared `Pairing.verifyOutcome` rule, never silently re-pinned.
struct PairView: View {
    @EnvironmentObject private var mesh: MeshService

    @State private var importText = ""
    @State private var parsed: Pairing.IdentityCode?
    @State private var parseFailed = false
    @State private var pinNote: PinNote?
    @State private var verified: [VerifiedPeer] = []
    @State private var shareItem: ShareItem?
    @State private var isShowingScanner = false

    private struct ShareItem: Identifiable { let text: String; var id: String { text } }
    private struct PinNote: Identifiable {
        let id = UUID()
        let text: String
        let isError: Bool
    }

    private var selfKeyWire: Data { mesh.router.identity.publicKeyWire }
    private var selfKeyHex: String { selfKeyWire.hex }
    private var identityCode: String { Pairing.encodeIdentityCode(publicKeyWire: selfKeyWire, name: mesh.displayName) }
    private var identityQR: UIImage? { QrCode.image(from: identityCode) }

    /// The 12-digit safety code for (me, imported peer) — shown whether or not pinned.
    private var parsedSafetyCode: String? {
        guard let parsed, let theirs = Data(hex: parsed.publicKeyWireHex) else { return nil }
        return Pairing.safetyCode(publicKeyWireA: selfKeyWire, publicKeyWireB: theirs)
    }
    private var parsedIsSelf: Bool { parsed?.publicKeyWireHex.lowercased() == selfKeyHex }
    /// Defence-in-depth: the imported key vs whatever the mesh peer table holds for that id.
    private var parsedMeshKeyMismatch: Bool {
        guard let parsed, let id = NodeId(hex: parsed.nodeIdHex), let peer = mesh.router.peer(id) else { return false }
        return peer.publicKeyWire.hex != parsed.publicKeyWireHex
    }

    var body: some View {
        Form {
            Section("Your identity code") {
                if let img = identityQR {
                    VStack(alignment: .center) {
                        Image(uiImage: img)
                            .interpolation(.none)
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: 220, maxHeight: 220)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .shadow(color: .black.opacity(0.08), radius: 6, x: 0, y: 3)
                            .accessibilityLabel("QR code of your Ripple identity code")
                            .frame(maxWidth: .infinity)
                    }
                    .padding(.vertical, 8)
                }

                Text(identityCode)
                    .font(.caption.monospaced())
                    .textSelection(.enabled)

                HStack(spacing: 12) {
                    Button {
                        UIPasteboard.general.string = identityCode
                    } label: {
                        Label("Copy", systemImage: "doc.on.doc")
                    }

                    Button {
                        shareItem = ShareItem(text: identityCode)
                    } label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                }
                .font(.body)

                Text("Show this QR to a nearby contact or share the code string over a secure trusted channel.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Section("Add & verify peer") {
                Button {
                    isShowingScanner = true
                } label: {
                    Label("Scan Peer QR Code", systemImage: "qrcode.viewfinder")
                        .font(.headline)
                }

                TextField("Or paste identity code (RIPPLE-ID:v1:…)", text: $importText, axis: .vertical)
                    .lineLimit(2...4)
                    .font(.caption.monospaced())
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .onChange(of: importText) { _, _ in
                        parsed = nil
                        parseFailed = false
                        pinNote = nil
                    }

                HStack {
                    Button("Validate Code") { importCode() }
                        .disabled(importText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    Spacer()
                }

                if parseFailed {
                    Text("Not a valid Ripple identity code — check the prefix, lengths and hex.")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }

                if let parsed {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(parsed.name ?? "Peer \(parsed.nodeIdHex.suffix(4))")
                                .font(.headline)
                            Spacer()
                            if parsedIsSelf {
                                Text("Self")
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.orange.opacity(0.2))
                                    .foregroundStyle(.orange)
                                    .clipShape(Capsule())
                            }
                        }

                        Text("Node ID: \(NodeId(hex: parsed.nodeIdHex)?.display ?? parsed.nodeIdHex)")
                            .font(.callout.monospaced())
                            .foregroundStyle(.secondary)

                        Text("Key: \(parsed.publicKeyWireHex.prefix(8))…\(parsed.publicKeyWireHex.suffix(8))")
                            .font(.caption.monospaced())
                            .foregroundStyle(.secondary)

                        if let safety = parsedSafetyCode {
                            Divider()
                            Text("Safety code (verify verbally or by sight):")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                            Text(safety)
                                .font(.title3.bold().monospaced())
                                .foregroundStyle(.primary)
                            Text("If both phones show the exact same numbers, you are protected against Man-in-the-Middle (MITM) attacks.")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }

                        if parsedIsSelf {
                            Text("That is your own identity code.").font(.footnote).foregroundStyle(.red)
                        }

                        if parsedMeshKeyMismatch {
                            Text("Key conflict: the mesh already knows a different key for this node id. Do NOT trust either until resolved in person.")
                                .font(.footnote).foregroundStyle(.red)
                        }

                        HStack {
                            Button("Pin as Verified") { pin() }
                                .buttonStyle(.borderedProminent)
                                .disabled(parsedIsSelf || parsedSafetyCode == nil)

                            Button("Copy Code") {
                                UIPasteboard.general.string = parsed.encode()
                            }
                            .buttonStyle(.bordered)
                        }
                        .padding(.top, 4)

                        if let pinNote {
                            Text(pinNote.text)
                                .font(.footnote)
                                .foregroundStyle(pinNote.isError ? .red : .green)
                        }
                    }
                    .padding(.vertical, 6)
                }
            }

            Section("Verified peers") {
                if verified.isEmpty {
                    Text("No pinned peers yet. Scan or import a peer's identity code above and pin after comparing safety codes.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                ForEach(verified) { v in verifiedRow(v) }
            }

            Section {
                Text("Pins are stored privately on this device and are never logged or transmitted. Un-pin only when your contact changes their cryptographic keys.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Pair & verify")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { verified = VerifiedPeers.all() }
        .sheet(isPresented: $isShowingScanner) {
            QRScannerSheet { scannedCode in
                importText = scannedCode
                importCode()
            }
        }
        .sheet(item: $shareItem) { item in ActivityView(items: [item.text]) }
    }

    @ViewBuilder
    private func verifiedRow(_ peer: VerifiedPeer) -> some View {
        let current = NodeId(hex: peer.nodeIdHex).flatMap { mesh.router.peer($0) }
        let mismatch = current.map { $0.publicKeyWire.hex != peer.publicKeyWireHex } ?? false
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(peer.name ?? "Peer \(peer.nodeIdHex.suffix(4))").font(.headline)
                    Text(NodeId(hex: peer.nodeIdHex)?.display ?? peer.nodeIdHex)
                        .font(.caption.monospaced()).foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    UIPasteboard.general.string = Pairing.IdentityCode(nodeIdHex: peer.nodeIdHex, publicKeyWireHex: peer.publicKeyWireHex, name: peer.name).encode()
                } label: { Image(systemName: "square.and.arrow.up") }
                .accessibilityLabel("Copy identity code")
                .buttonStyle(.borderless)
            }
            Text(peer.safetyCode).font(.body.monospaced())
            Text("Pinned \(Date(timeIntervalSince1970: Double(peer.addedAtMs) / 1000).formatted(date: .abbreviated, time: .shortened))")
                .font(.caption2).foregroundStyle(.secondary)
            if mismatch {
                Text("Key conflict: the mesh peer table currently has a different key for this node id.").font(.footnote).foregroundStyle(.red)
            }
        }
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                VerifiedPeers.unpin(nodeIdHex: peer.nodeIdHex)
                verified = VerifiedPeers.all()
            } label: { Label("Remove pin", systemImage: "trash") }
        }
    }

    private func importCode() {
        let code = importText.trimmingCharacters(in: .whitespacesAndNewlines)
        parsed = Pairing.decodeIdentityCode(code)
        parseFailed = parsed == nil
        pinNote = nil
    }

    private func pin() {
        guard let parsed, let safety = parsedSafetyCode else { return }
        let (outcome, _) = VerifiedPeers.pin(nodeIdHex: parsed.nodeIdHex, publicKeyWireHex: parsed.publicKeyWireHex,
                                             name: parsed.name, safetyCode: safety)
        switch outcome {
        case .verified:
            pinNote = PinNote(text: "Pinned — this peer is now verified on this device.", isError: false)
            importText = ""
            self.parsed = nil
        case .alreadyVerified:
            pinNote = PinNote(text: "Already pinned with the same key; record refreshed.", isError: false)
            importText = ""
            self.parsed = nil
        case .conflict:
            pinNote = PinNote(text: "Refused: this node id is pinned to a different key. Your old pin was kept — talk to your contact before trusting either key.", isError: true)
        }
        verified = VerifiedPeers.all()
    }
}

/// UIActivityViewController wrapper for the share sheet (same pattern as Diagnostics).
struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}

