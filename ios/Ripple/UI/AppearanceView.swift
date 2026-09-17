import SwiftUI

struct AppearanceView: View {
    @ObservedObject private var settings = AppearanceSettings.shared
    @State private var selectedTab: Int = 0 // 0: Dark, 1: Light, 2: Auto

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                // MARK: - Theme Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Theme")
                            .font(.headline.bold())
                        Spacer()
                        Text(settings.currentTheme.name)
                            .font(.caption.bold())
                            .foregroundStyle(settings.currentTheme.primaryColor)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(settings.currentTheme.primaryColor.opacity(0.15), in: RoundedRectangle(cornerRadius: 6))
                    }

                    // Category Tabs
                    HStack(spacing: 8) {
                        tabButton(title: "🌙 Dark", index: 0)
                        tabButton(title: "☀️ Light", index: 1)
                        tabButton(title: "⚙️ Auto", index: 2)
                    }

                    // Theme Cards Grid/List
                    let themes = displayedThemes
                    VStack(spacing: 8) {
                        ForEach(themes) { theme in
                            let isSelected = settings.themeId == theme.id
                            Button {
                                settings.themeId = theme.id
                            } label: {
                                HStack(spacing: 14) {
                                    // Gradient Swatch
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(LinearGradient(colors: theme.gradientColors, startPoint: .topLeading, endPoint: .bottomTrailing))
                                            .frame(width: 44, height: 36)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(Color.white.opacity(0.25), lineWidth: 1)
                                            )
                                        if isSelected {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 16, weight: .bold))
                                                .foregroundStyle(.white)
                                        }
                                    }

                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(theme.name)
                                            .font(.subheadline.weight(isSelected ? .bold : .medium))
                                            .foregroundStyle(Color.primary)
                                        HStack(spacing: 4) {
                                            Circle().fill(theme.primaryColor).frame(width: 8, height: 8)
                                            Circle().fill(theme.secondaryColor).frame(width: 8, height: 8)
                                            Circle().fill(theme.tertiaryColor).frame(width: 8, height: 8)
                                            Text(theme.isDark ? "Dark Palette" : "Light Palette")
                                                .font(.caption2)
                                                .foregroundStyle(.secondary)
                                        }
                                    }

                                    Spacer()

                                    if isSelected {
                                        Text("Active")
                                            .font(.caption2.bold())
                                            .foregroundStyle(theme.primaryColor)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(theme.primaryColor.opacity(0.15), in: Capsule())
                                    }
                                }
                                .padding(12)
                                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 14))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14)
                                        .stroke(isSelected ? theme.primaryColor : Color.clear, lineWidth: 1.5)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                Divider()

                // MARK: - Typography & Font Family
                VStack(alignment: .leading, spacing: 14) {
                    Text("Typography & Fonts")
                        .font(.headline.bold())

                    VStack(alignment: .leading, spacing: 6) {
                        Text("Font Family")
                            .font(.subheadline.bold())
                            .foregroundStyle(.secondary)

                        Menu {
                            ForEach(AppearanceSettings.fontOptions) { opt in
                                Button {
                                    settings.fontId = opt.id
                                } label: {
                                    HStack {
                                        Text(opt.name)
                                        if settings.fontId == opt.id {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(settings.currentFontOption.name)
                                        .font(settings.font(size: 15, weight: .semibold))
                                        .foregroundStyle(.primary)
                                    Text(settings.currentFontOption.previewSample)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.up.chevron.down")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    // Font Size Scaling (Continuous Volume Slider: 60% to 140%)
                    let percentage = Int((settings.fontScale * 100).rounded())
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Font Size Scaling")
                                .font(.subheadline.bold())
                                .foregroundStyle(.secondary)
                            Spacer()
                            Text("\(percentage)%" + (percentage == 100 ? " (Default)" : ""))
                                .font(.caption.bold())
                                .foregroundStyle(settings.currentTheme.primaryColor)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(settings.currentTheme.primaryColor.opacity(0.15), in: RoundedRectangle(cornerRadius: 6))
                        }

                        HStack(spacing: 12) {
                            Image(systemName: "textformat.size.smaller")
                                .font(.system(size: 14))
                                .foregroundStyle(.secondary)

                            Slider(value: $settings.fontScale, in: 0.60...1.40, step: 0.05)
                                .tint(settings.currentTheme.primaryColor)

                            Image(systemName: "textformat.size.larger")
                                .font(.system(size: 20))
                                .foregroundStyle(.secondary)
                        }

                        // Quick Scaling Presets
                        HStack(spacing: 8) {
                            presetChip(scale: 0.80, label: "80%")
                            presetChip(scale: 1.00, label: "100%")
                            presetChip(scale: 1.20, label: "120%")
                            presetChip(scale: 1.40, label: "140%")
                        }
                    }
                }

                Divider()

                // MARK: - Live Theme & Typography Preview Card
                VStack(alignment: .leading, spacing: 10) {
                    Text("Live Theme Preview")
                        .font(.headline.bold())

                    VStack(alignment: .leading, spacing: 12) {
                        // Incoming Bubble
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Asha (Peer · 1 hop)")
                                    .font(settings.font(size: 12, weight: .bold))
                                    .foregroundStyle(settings.currentTheme.primaryColor)
                                Text("Ripple mesh link verified! Testing live chat typography and gradient contrast.")
                                    .font(settings.font(size: 14))
                                    .foregroundStyle(.primary)
                                HStack {
                                    Spacer()
                                    Text("1:52 PM · BLE -68 dBm")
                                        .font(settings.font(size: 10))
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .padding(12)
                            .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 14))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            Spacer(minLength: 40)
                        }

                        // Outgoing Bubble
                        HStack {
                            Spacer(minLength: 40)
                            VStack(alignment: .trailing, spacing: 4) {
                                Text("Crystal clear colors & typography!")
                                    .font(settings.font(size: 14))
                                    .foregroundStyle(.white)
                                HStack(spacing: 4) {
                                    Text("1:53 PM · Delivered ✓✓")
                                        .font(settings.font(size: 10))
                                        .foregroundStyle(.white.opacity(0.85))
                                }
                            }
                            .padding(12)
                            .background(
                                LinearGradient(colors: settings.currentTheme.gradientColors, startPoint: .topLeading, endPoint: .bottomTrailing),
                                in: RoundedRectangle(cornerRadius: 14)
                            )
                        }
                    }
                    .padding(16)
                    .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 18))
                }
            }
            .padding(16)
        }
        .navigationTitle("Appearance & visual")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if settings.themeId == "system" {
                selectedTab = 2
            } else if settings.currentTheme.isDark {
                selectedTab = 0
            } else {
                selectedTab = 1
            }
        }
        .preferredColorScheme(settings.colorScheme)
        .tint(settings.currentTheme.primaryColor)
    }

    private var displayedThemes: [AppThemeOption] {
        switch selectedTab {
        case 0:
            return AppThemeOption.darkThemes
        case 1:
            return AppThemeOption.lightThemes
        default:
            return [AppThemeOption.systemTheme]
        }
    }

    private func tabButton(title: String, index: Int) -> some View {
        Button {
            selectedTab = index
            if index == 2 {
                settings.themeId = "system"
            } else if index == 0 && !settings.currentTheme.isDark {
                settings.themeId = AppThemeOption.darkThemes[0].id
            } else if index == 1 && settings.currentTheme.isDark {
                settings.themeId = AppThemeOption.lightThemes[0].id
            }
        } label: {
            Text(title)
                .font(.caption.bold())
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(selectedTab == index ? settings.currentTheme.primaryColor : Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(selectedTab == index ? .white : .primary)
        }
        .buttonStyle(.plain)
    }

    private func presetChip(scale: Double, label: String) -> some View {
        let isCurrent = Int((settings.fontScale * 100).rounded()) == Int((scale * 100).rounded())
        return Button {
            settings.fontScale = scale
        } label: {
            Text(label)
                .font(.caption2.bold())
                .frame(maxWidth: .infinity)
                .padding(.vertical, 6)
                .background(isCurrent ? settings.currentTheme.primaryColor : Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 8))
                .foregroundStyle(isCurrent ? .white : .secondary)
        }
        .buttonStyle(.plain)
    }
}
