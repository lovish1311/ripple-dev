import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var mesh: MeshService
    @ObservedObject private var settings = AppearanceSettings.shared
    @State private var name: String = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // MARK: - Profile Card
                    VStack(spacing: 12) {
                        HStack {
                            Text("Profile & display name")
                                .font(settings.font(size: 16, weight: .bold))
                            Spacer()
                            NavigationLink {
                                ProfileView()
                            } label: {
                                Text("Full Profile")
                                    .font(settings.font(size: 12, weight: .bold))
                                    .foregroundStyle(settings.currentTheme.primaryColor)
                            }
                        }

                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(settings.currentTheme.primaryColor.opacity(0.18))
                                    .frame(width: 44, height: 44)
                                Text(settings.avatar)
                                    .font(.system(size: 24))
                            }

                            TextField("Display name", text: $name)
                                .textFieldStyle(.roundedBorder)

                            Button("Save") {
                                mesh.setDisplayName(name.trimmingCharacters(in: .whitespaces))
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(settings.currentTheme.primaryColor)
                            .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || name.trimmingCharacters(in: .whitespaces) == mesh.displayName)
                        }
                    }
                    .padding(16)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))

                    // MARK: - Mesh Status
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Mesh status")
                            .font(settings.font(size: 16, weight: .bold))

                        Text("Bluetooth \(mesh.status.bluetoothOn ? "on" : "off") · \(mesh.status.directLinks) direct link\(mesh.status.directLinks == 1 ? "" : "s") · \(mesh.status.knownPeers) peer\(mesh.status.knownPeers == 1 ? "" : "s") known")
                            .font(settings.font(size: 13))
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))

                    // MARK: - Action Buttons Grid (Matches Android 2x2 layout)
                    VStack(spacing: 10) {
                        HStack(spacing: 10) {
                            NavigationLink { SosView() } label: {
                                actionGridCard(title: "SOS beacon", icon: "exclamationmark.octagon.fill", color: .red)
                            }
                            NavigationLink { PowerView() } label: {
                                actionGridCard(title: "Power profile", icon: "battery.75", color: Color(red: 0.1, green: 0.75, blue: 0.45))
                            }
                        }
                        HStack(spacing: 10) {
                            NavigationLink { DiagnosticsView() } label: {
                                actionGridCard(title: "Diagnostics", icon: "waveform.path.ecg", color: settings.currentTheme.primaryColor)
                            }
                            NavigationLink { FieldTestView() } label: {
                                actionGridCard(title: "Field test", icon: "checklist", color: .purple)
                            }
                        }
                    }

                    // MARK: - Detailed Settings Navigation Links
                    VStack(spacing: 10) {

                        NavigationLink {
                            AppearanceView()
                        } label: {
                            featureRow(
                                title: "Appearance & visual",
                                subtitle: "Themes (Dark/Light), font family, and font size scaling",
                                icon: "paintbrush.fill",
                                iconColor: .teal
                            )
                        }

                        NavigationLink {
                            PairView()
                        } label: {
                            featureRow(
                                title: "Pair & verify",
                                subtitle: "QR identity code, paste-import, pinned verified keys",
                                icon: "qrcode.viewfinder",
                                iconColor: .blue
                            )
                        }

                        NavigationLink {
                            BackupView()
                        } label: {
                            featureRow(
                                title: "Backup & restore",
                                subtitle: "Encrypted identity blob so a reinstall keeps your node id",
                                icon: "archivebox.fill",
                                iconColor: .orange
                            )
                        }
                    }

                    // Explanatory note
                    Text("Ripple never uses the internet. Messages hop phone-to-phone over Bluetooth LE, are signed by the sender, and direct messages are end-to-end encrypted. Messages for peers who are out of range are held and delivered when the mesh reconnects.")
                        .font(settings.font(size: 12))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 10)
                        .padding(.bottom, 20)
                }
                .padding(16)
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .onAppear {
                name = mesh.displayName
            }
        }
        .preferredColorScheme(settings.colorScheme)
        .tint(settings.currentTheme.primaryColor)
    }

    private func actionGridCard(title: String, icon: String, color: Color) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(color)
            Text(title)
                .font(settings.font(size: 14, weight: .semibold))
                .foregroundStyle(.primary)
            Spacer()
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private func featureRow(title: String, subtitle: String, icon: String, iconColor: Color) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundStyle(iconColor)
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(settings.font(size: 15, weight: .bold))
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(settings.font(size: 11))
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
