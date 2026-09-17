import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var mesh: MeshService
    @ObservedObject private var settings = AppearanceSettings.shared

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // MARK: - Profile Card (Matches Android 08_settings_screen.png)
                    NavigationLink {
                        ProfileView()
                    } label: {
                        HStack(spacing: 14) {
                            ZStack {
                                Circle()
                                    .fill(settings.currentTheme.primaryColor.opacity(0.18))
                                    .frame(width: 52, height: 52)
                                Text(settings.avatar)
                                    .font(.system(size: 28))
                            }

                            VStack(alignment: .leading, spacing: 3) {
                                Text(mesh.displayName.isEmpty ? "Anonymous Node" : mesh.displayName)
                                    .font(settings.font(size: 17, weight: .bold))
                                    .foregroundStyle(Color.primary)
                                Text("Tap to view & edit profile")
                                    .font(settings.font(size: 13))
                                    .foregroundStyle(.secondary)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Color.secondary.opacity(0.7))
                        }
                        .padding(16)
                        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)

                    // MARK: - App Preferences Section
                    settingsSection(title: "App Preferences") {
                        VStack(spacing: 10) {
                            NavigationLink {
                                AppearanceView()
                            } label: {
                                settingsRow(
                                    title: "Appearance & Themes",
                                    subtitle: "Customize light/dark palettes and typography",
                                    icon: "paintpalette.fill",
                                    iconColor: Color.purple,
                                    bgColor: Color.purple.opacity(0.12)
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                PowerView()
                            } label: {
                                settingsRow(
                                    title: "Power & Mesh Profile",
                                    subtitle: "Configure battery optimization & relay policy",
                                    icon: "battery.75",
                                    iconColor: Color(red: 0.1, green: 0.75, blue: 0.45),
                                    bgColor: Color(red: 0.1, green: 0.75, blue: 0.45).opacity(0.12)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // MARK: - Security & Network Section
                    settingsSection(title: "Security & Network") {
                        VStack(spacing: 10) {
                            NavigationLink {
                                PairView()
                            } label: {
                                settingsRow(
                                    title: "Pairing & Safety Numbers",
                                    subtitle: "Scan QR codes and verify contact safety numbers",
                                    icon: "qrcode.viewfinder",
                                    iconColor: Color(red: 0.9, green: 0.25, blue: 0.45),
                                    bgColor: Color(red: 0.9, green: 0.25, blue: 0.45).opacity(0.12)
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                BackupView()
                            } label: {
                                settingsRow(
                                    title: "Identity Backup & Restore",
                                    subtitle: "Encrypted export & recovery of P-256 keys",
                                    icon: "arrow.triangle.2.circlepath.circle.fill",
                                    iconColor: Color.blue,
                                    bgColor: Color.blue.opacity(0.12)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // MARK: - Diagnostics & Radio Tools Section
                    settingsSection(title: "Diagnostics & Radio Tools") {
                        VStack(spacing: 10) {
                            NavigationLink {
                                DiagnosticsView()
                            } label: {
                                settingsRow(
                                    title: "Diagnostics & Event Logs",
                                    subtitle: "View BLE links, frame counters & share logs",
                                    icon: "chart.bar.xaxis",
                                    iconColor: Color.orange,
                                    bgColor: Color.orange.opacity(0.12)
                                )
                            }
                            .buttonStyle(.plain)

                            NavigationLink {
                                FieldTestView()
                            } label: {
                                settingsRow(
                                    title: "Field Test Mode",
                                    subtitle: "Run automated mesh verification scenarios",
                                    icon: "checklist",
                                    iconColor: Color.indigo,
                                    bgColor: Color.indigo.opacity(0.12)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    // Explanatory note
                    Text("Ripple never uses the internet. Messages hop phone-to-phone over Bluetooth LE, are signed by the sender, and direct messages are end-to-end encrypted. Messages for peers who are out of range are held and delivered when the mesh reconnects.")
                        .font(settings.font(size: 11))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                        .padding(.top, 4)
                        .padding(.bottom, 20)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                }
            }
        }
        .preferredColorScheme(settings.colorScheme)
        .tint(settings.currentTheme.primaryColor)
    }

    private func settingsSection<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(settings.font(size: 13, weight: .bold))
                .foregroundStyle(settings.currentTheme.primaryColor)
                .padding(.horizontal, 4)

            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func settingsRow(title: String, subtitle: String, icon: String, iconColor: Color, bgColor: Color) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(bgColor)
                    .frame(width: 40, height: 40)
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(iconColor)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(settings.font(size: 15, weight: .bold))
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(settings.font(size: 11))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(Color.secondary.opacity(0.6))
        }
        .padding(12)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 14))
    }
}
