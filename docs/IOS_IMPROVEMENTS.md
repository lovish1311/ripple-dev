# iOS Improvements & Architectural Roadmap

This document defines the complete architectural roadmap and parity specifications for the Ripple iOS application (Swift, SwiftUI, SwiftData, iOS 17+) matching the production-grade Android implementation.

---

## 📋 Sector 1: Architecture & Swift Clean Architecture

- [ ] **Dependency Injection & Clean Architecture**:
  - Introduce domain protocols: `MeshRepositoryProtocol` and `DatabaseRepositoryProtocol`.
  - Refactor `MeshService` to decouple direct UI state from network/crypto layers.
  - Implement a lightweight DI container (Factory or Swift `@Observable` Environment injection) for testability.
- [ ] **SwiftData / Persistence Layer Optimization**:
  - Indexing: Ensure `@Attribute(.unique)` and compound index queries on `(conversation, timestamp)` and `(status)` to avoid main-thread IO latency.
  - Schema Migrations (v1 → v2):
    - Add `isEdited: Bool = false`
    - Add `deletedForEveryone: Bool = false`
    - Add `isForwarded: Bool = false`
- [ ] **Unit Testing Suite (XCTest / Swift Testing)**:
  - Add `MeshRepositoryTests` (mocking BLE transport and crypto).
  - Add `DatabaseTests` verifying queries, status updates, and soft deletes.

---

## 📋 Sector 2: Radio & Protocol Parity

- [ ] **CoreBluetooth Central/Peripheral Optimization**:
  - Serial queue flow-control tuning for `peripheralManagerIsReady` to maximize packet throughput over BLE MTU.
  - Efficient L2CAP and GATT chunking matching Android payload frames.
- [ ] **Background Mesh Keep-Alive**:
  - State restoration (`CBCentralManagerOptionRestoreIdentifierKey`, `CBPeripheralManagerOptionRestoreIdentifierKey`).
  - Optimize overflow service UUID discovery for iOS background scanning.

---

## 📋 Sector 3: UI & UX Parity (Matching Android)

- [ ] **WhatsApp-Style Delivery Status Indicators**:
  - Single tick (`✓`) for `SENT` / `PENDING`.
  - Double tick (`✓✓`) for `DELIVERED`.
  - Blue double tick (`✓✓` in accent blue) for `READ`.
  - Exclamation mark (`!`) for `FAILED`.
  - Dynamic type and zero hardcoded colors (Asset Catalog semantic colors).
- [ ] **Message Long-Press Context Menu & Actions**:
  - **Copy**: Copy message text to `UIPasteboard` with haptic feedback.
  - **Edit**: Inline edit field with `(edited)` suffix tag.
  - **Delete For Me**: Soft-delete with 4-second animated undo toast.
  - **Delete For Everyone**: Tombstone message ("🚫 This message was deleted").
  - **Forward Message**: Searchable Sheet presenting contacts with Hop count Chips (`Direct`, `1 hop`, `2+ hops`), setting `isForwarded = true` & `➡ Forwarded` UI tag.
- [ ] **HomeScreen Modernization**:
  - **Segmented Pill Tabs**: Animated pill switcher between `Chats` and `Peers` with unread/peer badges.
  - **Mesh Status Pill**: Top navigation bar badge indicating BLE active status and connected peer count.
  - **SOS Floating Action Button**: Prominent circular FAB triggering SOS broadcast sheet with location toggle.

---

## 🟢 Sector 4: Priority 1 — QR Code Pairing & Safety Numbers (Phase 0.2) (COMPLETED)

- [x] **QR Code Generation**:
  - Generated QR code using CoreImage `CIQRCodeGenerator` with 3x retina scaling in `QrCode.swift` for URI format: `RIPPLE-ID:v1:<nodeIdHex>:<publicKeyWireHex>[:<name>]`.
  - Provided copy and share sheet actions in `PairView.swift`.
- [x] **AVFoundation QR Camera Scanner**:
  - Implemented `QRScannerView.swift` with real-time `AVCaptureMetadataOutput` scanning for `.qr`.
  - Viewfinder overlay with animated laser line, corner reticles, torch/flashlight toggle, haptic feedback, and camera permission handling.
  - Added `NSCameraUsageDescription` to `Info.plist`.
- [x] **Cryptographic Safety Number Verification**:
  - SHA-256 derivation over lexicographically sorted 65-byte public keys (`Pairing.safetyCode(publicKeyWireA, publicKeyWireB)`).
  - Formatted 12-digit numeric safety code grouped 4-4-4 (e.g., `5046 5756 7335`) for symmetrical out-of-band MITM defense.
- [x] **Public Key Pinning & Conflict Detection**:
  - `VerifiedPeers.swift` persistent private store in `UserDefaults`.
  - Prevents MITM attacks with `Pairing.VerifyOutcome` (`.verified`, `.alreadyVerified`, `.conflict`).
- [x] **Unit Testing Suite**:
  - Verified in `PairingTests.swift` with 100% vector conformance matching Android vectors.

---

## 📋 Sector 5: Priority 2 — Encrypted Identity Backup & Restore (Phase 0.3)

- [ ] **CryptoKit & CommonCrypto Encryption Pipeline**:
  - **Key Derivation**: PBKDF2-HMAC-SHA256 with 600,000 iterations, 16-byte random salt (`CCKeyDerivationPBKDF`).
  - **Encryption**: AES-256-GCM authenticated encryption with 12-byte IV (`AES.GCM.seal`).
- [ ] **Serialization & Format Compatibility**:
  - Export/Import format: `RIPPLE-BKP:v1:<base64-salt>:<base64-iv>:<base64-ciphertext>`.
- [ ] **Backup & Restore UI**:
  - Export screen with password confirmation, copy/share encrypted backup payload.
  - Import screen with password prompt, decrypt, validate, and restore identity.
- [ ] **Unit Tests**:
  - Verify round-trip encryption/decryption and cross-compatibility with Android backup strings.

---

## 📋 Sector 6: Emergency SOS Hub & Priority Pinned Cards (Planned for macOS Setup)

- [ ] **SwiftData SOS Schema & Repository Integration**:
  - Add `@Model class SosBeacon`:
    - `id: String` (Primary Key)
    - `senderId: String`, `senderName: String`
    - `timestamp: Date`, `latitude: Double?`, `longitude: Double?`
    - `status: SosStatus` (`.active`, `.acknowledged`, `.resolved`)
    - `isVerified: Bool`
- [ ] **Priority Pinned SOS Banner & Cards (SwiftUI)**:
  - High-priority pinned section at top of `HomeView` above conversation list.
  - Single active emergency renders an alert card with **Open Thread** and **Acknowledge** buttons (dispatches BLE ACK packet).
- [ ] **Master Alert Banner & SOS Hub Sheet**:
  - If $\ge 2$ active emergencies exist, collapse into a compact banner ("🚨 Active Emergencies (N)") with a **View All (N)** action.
  - Present `EmergencySosHubView` sheet listing all beacon cards with full triage telemetry (GPS map link, hop count, status badges).

---

## 📋 Sector 7: WhatsApp-Style Slide-to-Reply & Quoted Replies (Planned for macOS Setup)

- [ ] **SwiftUI Drag Gestures & Spring Physics**:
  - `DragGesture(minimumDistance: 15)` attached to message bubble views:
    - **Slide Right** on received messages (`translation.width > 0`) reveals animated reply arrow (↩️) with `UIImpactFeedbackGenerator(style: .medium)`.
    - **Slide Left** on sent messages (`translation.width < 0`) triggers reply mode targeting user's own message.
    - Uses `.animation(.spring(response: 0.35, dampingFraction: 0.7), value: dragOffset)` to snap back smoothly.
- [ ] **Context Menu & Long-Press Actions**:
  - Message bubble `.contextMenu` featuring **Reply (↩️)**, **Copy (📋)**, **Edit (✏️)**, **Forward (➡️)**, and **Delete (🗑️)**.
- [ ] **Replying Preview Dock Bar**:
  - Docked directly above the chat `TextField` input bar in `ChatView`.
  - Shows `Replying to <Name>`, snippet preview, vertical accent line, and `Button(action: cancelReply) { Image(systemName: "xmark") }`.
- [ ] **Quoted Message Bubble UI**:
  - Embedded container at top of message bubble with vertical accent bar, quoted sender name, and 2-line snippet.
- [ ] **SwiftData Model & BLE Protocol Serialization**:
  - Update `Message` model with `replyToId: String?`, `replySender: String?`, `replySnippet: String?`.
  - Serialize/deserialize into binary mesh wire packet payload matching Android frame structure.

---

## 📋 Sector 8: Dedicated Profile Screen, Settings Modernization & Avatar Mesh Protocol (Planned for macOS Setup)

- [ ] **Modernized Line-by-Line `SettingsView.swift` (SwiftUI `List`)**:
  - Top Profile Row: `NavigationLink(destination: ProfileView())` displaying user avatar, name, and truncated Node ID.
  - Line-by-line navigation rows with system SF Symbols:
    - 🎨 Appearance & Themes
    - ⚡ Power & Relay Mode
    - 🔐 Pairing & Safety Numbers
    - 💾 Identity Backup & Restore
    - 📊 Diagnostics & Radio Logs
    - 📡 Field Test RF Tool
    - 🗑️ Reset Local Database (Destructive action sheet)
- [ ] **Dedicated Profile & Identity View (`ProfileView.swift`)**:
  - Avatar carousel & quick selector with emoji avatars (`👨‍🚀`, `👩‍💻`, `🧑‍🚀`, `🐱`, `🦊`, etc.).
  - Display name text field with live validation and SwiftData update.
  - Full Node ID card with 1-tap clipboard copy and QR code modal shortcut.
  - Public Key hexadecimal and hardware security status.
- [ ] **Avatar Wire Mesh Propagation Protocol (`AvatarHelper.swift`)**:
  - **Zero Breaking Wire Changes**: Formats wire name as `"${avatar} ${name}"` (e.g. `"🦊 Tablet"`, `"👩‍💻 Rip"`).
  - **Extractor Utility**: Extracts avatar emoji string and clean display name on receiving `Announce` frames.
  - **SwiftData Peer Schema Migration**: Add `avatar: String? = nil` to `Peer` model.
  - **Adaptive Squircle Avatar Badge**: Render avatar emoji in rounded squircle with deterministic pastel gradient derived from peer's `nodeIdHex`.
  - Display in `HomeView` (Chats & Peers lists), `ChatView` (Navigation TopBar), and `ForwardMessageView`.
- [ ] **Immediate Re-Announce on Mutation**:
  - When user edits their avatar or name in `ProfileView`, immediately trigger an outgoing `Announce` frame over `CoreBluetooth`.

---

## 📋 Sector 9: Micro-Profile Picture Protocol (<1.5 KB On-Demand BLE GATT) (PLANNED)

- [ ] **Deterministic Vector Avatar Seed**: 1-byte seed in peer discovery beacons for zero network MTU impact.
- [ ] **Micro-Profile Picture Transfer (<1.5 KB)**: Ultra-compact 64x64 / 96x96 WebP compressed profile images exchanged over on-demand GATT request upon mutual contact pairing.
- [ ] **Local SwiftData Image Cache**: Cached with SHA-256 content hashes to eliminate redundant radio transmissions.



