import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var mesh: MeshService
    @ObservedObject private var settings = AppearanceSettings.shared
    @State private var name: String = ""
    @State private var isEditingName: Bool = false
    @State private var copied: Bool = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // MARK: - Atmospheric Hero Profile Card
                VStack(spacing: 16) {
                    ZStack {
                        // Atmospheric Radial Gradient Glow
                        RadialGradient(
                            colors: [
                                settings.currentTheme.primaryColor.opacity(0.35),
                                settings.currentTheme.secondaryColor.opacity(0.15),
                                Color.clear
                            ],
                            center: .center,
                            startRadius: 20,
                            endRadius: 85
                        )
                        .frame(width: 170, height: 170)

                        // Outer Glowing Ring
                        Circle()
                            .stroke(
                                LinearGradient(
                                    colors: settings.currentTheme.gradientColors,
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: 3
                            )
                            .frame(width: 104, height: 104)

                        // Avatar Circle
                        Circle()
                            .fill(Color(.systemBackground))
                            .frame(width: 96, height: 96)
                            .shadow(color: settings.currentTheme.primaryColor.opacity(0.2), radius: 10)

                        Text(settings.avatar)
                            .font(.system(size: 52))
                    }
                    .padding(.top, 8)

                    // Display Name & Edit Section
                    VStack(spacing: 6) {
                        if isEditingName {
                            HStack(spacing: 8) {
                                TextField("Enter your handle", text: $name)
                                    .textFieldStyle(.roundedBorder)
                                    .frame(maxWidth: 220)

                                Button("Save") {
                                    let trimmed = name.trimmingCharacters(in: .whitespaces)
                                    if !trimmed.isEmpty {
                                        mesh.setDisplayName(trimmed)
                                    }
                                    isEditingName = false
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(settings.currentTheme.primaryColor)
                            }
                        } else {
                            Button {
                                isEditingName = true
                            } label: {
                                HStack(spacing: 6) {
                                    Text(mesh.displayName.isEmpty ? "Anonymous Node" : mesh.displayName)
                                        .font(settings.font(size: 22, weight: .bold))
                                        .foregroundStyle(.primary)

                                    Image(systemName: "pencil.circle.fill")
                                        .font(.title3)
                                        .foregroundStyle(settings.currentTheme.primaryColor)
                                }
                            }
                        }

                        // Status Badge
                        HStack(spacing: 6) {
                            Circle()
                                .fill(Color(red: 0.1, green: 0.8, blue: 0.5))
                                .frame(width: 8, height: 8)
                            Text("Ready to broadcast")
                                .font(settings.font(size: 12, weight: .semibold))
                                .foregroundStyle(Color(red: 0.1, green: 0.8, blue: 0.5))
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color(red: 0.1, green: 0.8, blue: 0.5).opacity(0.12), in: Capsule())
                    }
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 24))

                // MARK: - Avatar Preset Selection
                VStack(alignment: .leading, spacing: 10) {
                    Text("Choose Avatar")
                        .font(settings.font(size: 16, weight: .bold))

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(AppearanceSettings.avatarPresets, id: \.self) { emo in
                                let isSelected = settings.avatar == emo
                                Button {
                                    settings.avatar = emo
                                } label: {
                                    ZStack {
                                        Circle()
                                            .fill(isSelected ? settings.currentTheme.primaryColor.opacity(0.18) : Color(.systemBackground))
                                            .frame(width: 52, height: 52)
                                        Text(emo)
                                            .font(.system(size: 28))
                                        if isSelected {
                                            Circle()
                                                .stroke(settings.currentTheme.primaryColor, lineWidth: 2)
                                                .frame(width: 52, height: 52)
                                        }
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .padding(16)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 18))

                // MARK: - Cryptographic Mesh Node ID Card
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        HStack(spacing: 10) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(settings.currentTheme.primaryColor.opacity(0.15))
                                    .frame(width: 38, height: 38)
                                Image(systemName: "key.fill")
                                    .font(.system(size: 18))
                                    .foregroundStyle(settings.currentTheme.primaryColor)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Mesh Node ID")
                                    .font(settings.font(size: 16, weight: .bold))
                                Text("Hardware-backed public identity key")
                                    .font(settings.font(size: 12))
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Spacer()
                        Button {
                            UIPasteboard.general.string = mesh.router.selfId.hex
                            withAnimation { copied = true }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                withAnimation { copied = false }
                            }
                        } label: {
                            Image(systemName: copied ? "checkmark" : "doc.on.doc.fill")
                                .font(.system(size: 16))
                                .foregroundStyle(copied ? Color.green : settings.currentTheme.primaryColor)
                                .padding(8)
                                .background(Color(.systemBackground), in: Circle())
                        }
                    }

                    // Monospace ID Container
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(mesh.router.selfId.display)
                                .font(.system(size: 17, weight: .bold, design: .monospaced))
                                .foregroundStyle(settings.currentTheme.primaryColor)
                            Spacer()
                            Text("ECDSA P-256")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundStyle(settings.currentTheme.primaryColor)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(settings.currentTheme.primaryColor.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
                        }

                        Text(mesh.router.selfId.hex)
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                    .padding(14)
                    .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 14))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(Color(.separator).opacity(0.4), lineWidth: 1)
                    )

                    if copied {
                        Text("Node ID copied to clipboard")
                            .font(settings.font(size: 12, weight: .bold))
                            .foregroundStyle(Color.green)
                    }

                    // Security Footer
                    HStack(spacing: 6) {
                        Image(systemName: "lock.fill")
                            .font(.caption2)
                            .foregroundStyle(Color(red: 0.1, green: 0.8, blue: 0.5))
                        Text("End-to-end encrypted with local Secure Enclave keys")
                            .font(settings.font(size: 12))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.top, 2)
                }
                .padding(18)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 20))

                // MARK: - Key Management & Backup Navigation
                VStack(spacing: 10) {

                    NavigationLink { BackupView() } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "archivebox.fill")
                                .font(.title3)
                                .foregroundStyle(Color.orange)
                                .frame(width: 32)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Identity Backup & Recovery")
                                    .font(settings.font(size: 15, weight: .bold))
                                    .foregroundStyle(.primary)
                                Text("Export encrypted backup blob")
                                    .font(settings.font(size: 12))
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(14)
                        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 14))
                    }
                }
            }
            .padding(16)
        }
        .navigationTitle("Profile & identity")
        .navigationBarTitleDisplayMode(.inline)
        .preferredColorScheme(settings.colorScheme)
        .tint(settings.currentTheme.primaryColor)
        .onAppear {
            name = mesh.displayName
        }
    }
}
