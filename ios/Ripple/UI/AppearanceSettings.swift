import SwiftUI

public struct AppThemeOption: Identifiable {
    public let id: String
    public let name: String
    public let isDark: Bool
    public let gradientColors: [Color]
    public let primaryColor: Color
    public let secondaryColor: Color
    public let tertiaryColor: Color
    public let backgroundColor: Color
    public let surfaceColor: Color

    public static let darkThemes: [AppThemeOption] = [
        AppThemeOption(
            id: "cyber_ice",
            name: "Cyber Ice",
            isDark: true,
            gradientColors: [Color(red: 0.0, green: 0.85, blue: 0.95), Color(red: 0.15, green: 0.45, blue: 0.95)],
            primaryColor: Color(red: 0.0, green: 0.85, blue: 0.95),
            secondaryColor: Color(red: 0.15, green: 0.45, blue: 0.95),
            tertiaryColor: Color(red: 0.2, green: 0.9, blue: 0.6),
            backgroundColor: Color(red: 0.02, green: 0.05, blue: 0.1),
            surfaceColor: Color(red: 0.05, green: 0.1, blue: 0.18)
        ),
        AppThemeOption(
            id: "nebula",
            name: "Nebula",
            isDark: true,
            gradientColors: [Color(red: 0.65, green: 0.35, blue: 0.95), Color(red: 0.95, green: 0.25, blue: 0.6)],
            primaryColor: Color(red: 0.65, green: 0.35, blue: 0.95),
            secondaryColor: Color(red: 0.95, green: 0.25, blue: 0.6),
            tertiaryColor: Color(red: 0.5, green: 0.3, blue: 0.9),
            backgroundColor: Color(red: 0.06, green: 0.02, blue: 0.1),
            surfaceColor: Color(red: 0.12, green: 0.05, blue: 0.18)
        ),
        AppThemeOption(
            id: "midnight",
            name: "Midnight",
            isDark: true,
            gradientColors: [Color(red: 0.2, green: 0.5, blue: 0.98), Color(red: 0.35, green: 0.3, blue: 0.95)],
            primaryColor: Color(red: 0.2, green: 0.5, blue: 0.98),
            secondaryColor: Color(red: 0.35, green: 0.3, blue: 0.95),
            tertiaryColor: Color(red: 0.1, green: 0.75, blue: 0.9),
            backgroundColor: Color(red: 0.03, green: 0.05, blue: 0.12),
            surfaceColor: Color(red: 0.06, green: 0.09, blue: 0.2)
        ),
        AppThemeOption(
            id: "forest",
            name: "Forest",
            isDark: true,
            gradientColors: [Color(red: 0.06, green: 0.72, blue: 0.5), Color(red: 0.02, green: 0.71, blue: 0.83)],
            primaryColor: Color(red: 0.06, green: 0.72, blue: 0.5),
            secondaryColor: Color(red: 0.02, green: 0.71, blue: 0.83),
            tertiaryColor: Color(red: 0.2, green: 0.83, blue: 0.6),
            backgroundColor: Color(red: 0.01, green: 0.08, blue: 0.05),
            surfaceColor: Color(red: 0.02, green: 0.18, blue: 0.12)
        ),
        AppThemeOption(
            id: "solar",
            name: "Solar",
            isDark: true,
            gradientColors: [Color(red: 0.98, green: 0.57, blue: 0.24), Color(red: 0.96, green: 0.25, blue: 0.37)],
            primaryColor: Color(red: 0.98, green: 0.57, blue: 0.24),
            secondaryColor: Color(red: 0.96, green: 0.25, blue: 0.37),
            tertiaryColor: Color(red: 0.98, green: 0.75, blue: 0.14),
            backgroundColor: Color(red: 0.08, green: 0.02, blue: 0.0),
            surfaceColor: Color(red: 0.15, green: 0.05, blue: 0.02)
        ),
        AppThemeOption(
            id: "obsidian",
            name: "Obsidian",
            isDark: true,
            gradientColors: [Color(red: 0.89, green: 0.91, blue: 0.94), Color(red: 0.58, green: 0.64, blue: 0.72)],
            primaryColor: Color(red: 0.89, green: 0.91, blue: 0.94),
            secondaryColor: Color(red: 0.58, green: 0.64, blue: 0.72),
            tertiaryColor: Color(red: 0.22, green: 0.74, blue: 0.97),
            backgroundColor: Color(red: 0.04, green: 0.05, blue: 0.08),
            surfaceColor: Color(red: 0.09, green: 0.11, blue: 0.13)
        ),
        AppThemeOption(
            id: "aurora",
            name: "Aurora",
            isDark: true,
            gradientColors: [Color(red: 0.18, green: 0.83, blue: 0.75), Color(red: 0.65, green: 0.55, blue: 0.98)],
            primaryColor: Color(red: 0.18, green: 0.83, blue: 0.75),
            secondaryColor: Color(red: 0.65, green: 0.55, blue: 0.98),
            tertiaryColor: Color(red: 0.29, green: 0.87, blue: 0.5),
            backgroundColor: Color(red: 0.02, green: 0.06, blue: 0.09),
            surfaceColor: Color(red: 0.04, green: 0.15, blue: 0.2)
        ),
        AppThemeOption(
            id: "dark",
            name: "Dark Mesh",
            isDark: true,
            gradientColors: [Color(red: 0.5, green: 0.86, blue: 0.79), Color(red: 0.61, green: 0.77, blue: 1.0)],
            primaryColor: Color(red: 0.5, green: 0.86, blue: 0.79),
            secondaryColor: Color(red: 0.61, green: 0.77, blue: 1.0),
            tertiaryColor: Color(red: 1.0, green: 0.71, blue: 0.62),
            backgroundColor: Color(red: 0.07, green: 0.07, blue: 0.07),
            surfaceColor: Color(red: 0.12, green: 0.12, blue: 0.12)
        )
    ]

    public static let lightThemes: [AppThemeOption] = [
        AppThemeOption(
            id: "lavender_mist",
            name: "Lavender Mist",
            isDark: false,
            gradientColors: [Color(red: 0.49, green: 0.23, blue: 0.93), Color(red: 0.86, green: 0.15, blue: 0.47)],
            primaryColor: Color(red: 0.49, green: 0.23, blue: 0.93),
            secondaryColor: Color(red: 0.86, green: 0.15, blue: 0.47),
            tertiaryColor: Color(red: 0.31, green: 0.27, blue: 0.9),
            backgroundColor: Color(red: 0.98, green: 0.96, blue: 1.0),
            surfaceColor: Color(red: 0.95, green: 0.91, blue: 1.0)
        ),
        AppThemeOption(
            id: "nordic_frost",
            name: "Nordic Frost",
            isDark: false,
            gradientColors: [Color(red: 0.01, green: 0.52, blue: 0.78), Color(red: 0.05, green: 0.58, blue: 0.53)],
            primaryColor: Color(red: 0.01, green: 0.52, blue: 0.78),
            secondaryColor: Color(red: 0.05, green: 0.58, blue: 0.53),
            tertiaryColor: Color(red: 0.39, green: 0.4, blue: 0.95),
            backgroundColor: Color(red: 0.94, green: 0.98, blue: 1.0),
            surfaceColor: Color(red: 0.88, green: 0.95, blue: 1.0)
        ),
        AppThemeOption(
            id: "mint_breeze",
            name: "Mint Breeze",
            isDark: false,
            gradientColors: [Color(red: 0.02, green: 0.59, blue: 0.41), Color(red: 0.01, green: 0.52, blue: 0.78)],
            primaryColor: Color(red: 0.02, green: 0.59, blue: 0.41),
            secondaryColor: Color(red: 0.01, green: 0.52, blue: 0.78),
            tertiaryColor: Color(red: 0.09, green: 0.64, blue: 0.29),
            backgroundColor: Color(red: 0.94, green: 0.99, blue: 0.96),
            surfaceColor: Color(red: 0.86, green: 0.99, blue: 0.91)
        ),
        AppThemeOption(
            id: "solar_amber",
            name: "Solar Amber",
            isDark: false,
            gradientColors: [Color(red: 0.85, green: 0.47, blue: 0.02), Color(red: 0.92, green: 0.35, blue: 0.05)],
            primaryColor: Color(red: 0.85, green: 0.47, blue: 0.02),
            secondaryColor: Color(red: 0.92, green: 0.35, blue: 0.05),
            tertiaryColor: Color(red: 0.86, green: 0.15, blue: 0.15),
            backgroundColor: Color(red: 1.0, green: 0.98, blue: 0.92),
            surfaceColor: Color(red: 1.0, green: 0.95, blue: 0.78)
        ),
        AppThemeOption(
            id: "rose_sunset",
            name: "Rose Sunset",
            isDark: false,
            gradientColors: [Color(red: 0.88, green: 0.11, blue: 0.28), Color(red: 0.98, green: 0.45, blue: 0.09)],
            primaryColor: Color(red: 0.88, green: 0.11, blue: 0.28),
            secondaryColor: Color(red: 0.98, green: 0.45, blue: 0.09),
            tertiaryColor: Color(red: 0.75, green: 0.09, blue: 0.36),
            backgroundColor: Color(red: 1.0, green: 0.95, blue: 0.95),
            surfaceColor: Color(red: 1.0, green: 0.89, blue: 0.9)
        ),
        AppThemeOption(
            id: "light",
            name: "Light Mesh",
            isDark: false,
            gradientColors: [Color(red: 0.0, green: 0.42, blue: 0.37), Color(red: 0.23, green: 0.37, blue: 0.55)],
            primaryColor: Color(red: 0.0, green: 0.42, blue: 0.37),
            secondaryColor: Color(red: 0.23, green: 0.37, blue: 0.55),
            tertiaryColor: Color(red: 0.61, green: 0.26, blue: 0.15),
            backgroundColor: Color(red: 0.97, green: 0.98, blue: 0.98),
            surfaceColor: Color(red: 0.93, green: 0.95, blue: 0.96)
        )
    ]

    public static let systemTheme = AppThemeOption(
        id: "system",
        name: "System Default",
        isDark: false,
        gradientColors: [Color.blue, Color.purple],
        primaryColor: Color.accentColor,
        secondaryColor: Color.purple,
        tertiaryColor: Color.blue,
        backgroundColor: Color(.systemBackground),
        surfaceColor: Color(.secondarySystemBackground)
    )

    public static var all: [AppThemeOption] {
        darkThemes + lightThemes + [systemTheme]
    }
}

public struct AppFontOption: Identifiable {
    public let id: String
    public let name: String
    public let previewSample: String
}

public final class AppearanceSettings: ObservableObject {
    public static let shared = AppearanceSettings()

    public static let fontOptions: [AppFontOption] = [
        AppFontOption(id: "system", name: "Default System", previewSample: "San Francisco"),
        AppFontOption(id: "chiller", name: "Chiller / Cursive", previewSample: "Cursive Script"),
        AppFontOption(id: "monospace", name: "Monospace", previewSample: "SF Mono / Code"),
        AppFontOption(id: "serif", name: "Serif", previewSample: "New York Serif"),
        AppFontOption(id: "sans_serif", name: "Sans-Serif", previewSample: "SF Pro Sans")
    ]

    public static let avatarPresets = [
        "👦", "👧", "👨‍🚀", "👩‍💻", "🧑‍🎤", "🧔", "👩‍⚕️", "🕵️",
        "🐱", "🐶", "🦊", "🐼", "🦁", "🐺", "🐰", "🐻"
    ]

    @Published public var themeId: String {
        didSet { UserDefaults.standard.set(themeId, forKey: "ripple.themeId") }
    }
    @Published public var fontId: String {
        didSet { UserDefaults.standard.set(fontId, forKey: "ripple.fontId") }
    }
    @Published public var fontScale: Double {
        didSet { UserDefaults.standard.set(fontScale, forKey: "ripple.fontScale") }
    }
    @Published public var avatar: String {
        didSet { UserDefaults.standard.set(avatar, forKey: "ripple.avatar") }
    }

    public init() {
        self.themeId = UserDefaults.standard.string(forKey: "ripple.themeId") ?? "cyber_ice"
        self.fontId = UserDefaults.standard.string(forKey: "ripple.fontId") ?? "system"
        let savedScale = UserDefaults.standard.double(forKey: "ripple.fontScale")
        self.fontScale = savedScale > 0.1 ? savedScale : 1.0
        self.avatar = UserDefaults.standard.string(forKey: "ripple.avatar") ?? "👨‍🚀"
    }

    public var currentTheme: AppThemeOption {
        AppThemeOption.all.first { $0.id == themeId } ?? AppThemeOption.darkThemes[0]
    }

    public var currentFontOption: AppFontOption {
        Self.fontOptions.first { $0.id == fontId } ?? Self.fontOptions[0]
    }

    public var colorScheme: ColorScheme? {
        if themeId == "system" { return nil }
        return currentTheme.isDark ? .dark : .light
    }

    public func font(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        let scaledSize = size * fontScale
        switch fontId {
        case "chiller":
            return Font.custom("Snell Roundhand", size: scaledSize).weight(weight == .regular ? .bold : weight)
        case "monospace":
            return Font.system(size: scaledSize, weight: weight, design: .monospaced)
        case "serif":
            return Font.system(size: scaledSize, weight: weight, design: .serif)
        default:
            return Font.system(size: scaledSize, weight: weight, design: .default)
        }
    }
}
