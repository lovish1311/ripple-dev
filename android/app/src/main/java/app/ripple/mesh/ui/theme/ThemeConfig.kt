package app.ripple.mesh.ui.theme

import androidx.compose.ui.graphics.Color
import app.ripple.mesh.R

enum class AppTheme(
    val id: String,
    val titleResId: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val cardColor: Color,
    val textColor: Color,
    val textSecondaryColor: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val gradientColors: List<Color>,
    val isDark: Boolean
) {
    // ⚙️ Dynamic System Theme
    SYSTEM(
        id = "system",
        titleResId = R.string.theme_system,
        primaryColor = Color(0xFF006B5E),
        secondaryColor = Color(0xFF3B5E8C),
        tertiaryColor = Color(0xFF9C4325),
        backgroundColor = Color(0xFFF8F9FA),
        surfaceColor = Color(0xFFEEF2F6),
        cardColor = Color(0xFFE2E8F0),
        textColor = Color(0xFF0F172A),
        textSecondaryColor = Color(0xFF64748B),
        primaryContainer = Color(0xFFCCE8E3),
        onPrimaryContainer = Color(0xFF003730),
        gradientColors = listOf(Color(0xFF006B5E), Color(0xFF3B5E8C)),
        isDark = false
    ),

    // 🌙 DARK THEMES (Deep, immersive dark canvases with vibrant neon / gradient accents)
    CYBER_ICE(
        id = "cyber_ice",
        titleResId = R.string.theme_cyber_ice,
        primaryColor = Color(0xFF00F2FE),
        secondaryColor = Color(0xFF38BDF8),
        tertiaryColor = Color(0xFF818CF8),
        backgroundColor = Color(0xFF050B14),
        surfaceColor = Color(0xFF0C1829),
        cardColor = Color(0xFF14243B),
        textColor = Color(0xFFF0FDF4),
        textSecondaryColor = Color(0xFF94A3B8),
        primaryContainer = Color(0xFF0C2D48),
        onPrimaryContainer = Color(0xFF7DD3FC),
        gradientColors = listOf(Color(0xFF00F2FE), Color(0xFF38BDF8), Color(0xFF818CF8)),
        isDark = true
    ),
    NEBULA(
        id = "nebula",
        titleResId = R.string.theme_nebula,
        primaryColor = Color(0xFFC084FC),
        secondaryColor = Color(0xFFF472B6),
        tertiaryColor = Color(0xFF38BDF8),
        backgroundColor = Color(0xFF090414),
        surfaceColor = Color(0xFF160B29),
        cardColor = Color(0xFF24123E),
        textColor = Color(0xFFF8FAFC),
        textSecondaryColor = Color(0xFFCBD5E1),
        primaryContainer = Color(0xFF3B1366),
        onPrimaryContainer = Color(0xFFF3E8FF),
        gradientColors = listOf(Color(0xFFC084FC), Color(0xFFF472B6)),
        isDark = true
    ),
    MIDNIGHT(
        id = "midnight",
        titleResId = R.string.theme_midnight,
        primaryColor = Color(0xFF60A5FA),
        secondaryColor = Color(0xFF818CF8),
        tertiaryColor = Color(0xFF34D399),
        backgroundColor = Color(0xFF000000),
        surfaceColor = Color(0xFF121212),
        cardColor = Color(0xFF1E1E1E),
        textColor = Color(0xFFFFFFFF),
        textSecondaryColor = Color(0xFFA1A1AA),
        primaryContainer = Color(0xFF1E293B),
        onPrimaryContainer = Color(0xFF93C5FD),
        gradientColors = listOf(Color(0xFF60A5FA), Color(0xFF818CF8)),
        isDark = true
    ),
    FOREST(
        id = "forest",
        titleResId = R.string.theme_forest,
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF06B6D4),
        tertiaryColor = Color(0xFF34D399),
        backgroundColor = Color(0xFF02140D),
        surfaceColor = Color(0xFF062E1E),
        cardColor = Color(0xFF0D422C),
        textColor = Color(0xFFECFDF5),
        textSecondaryColor = Color(0xFFA7F3D0),
        primaryContainer = Color(0xFF064E3B),
        onPrimaryContainer = Color(0xFF6EE7B7),
        gradientColors = listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
        isDark = true
    ),
    SOLAR(
        id = "solar",
        titleResId = R.string.theme_solar,
        primaryColor = Color(0xFFFB923C),
        secondaryColor = Color(0xFFF43F5E),
        tertiaryColor = Color(0xFFFBBF24),
        backgroundColor = Color(0xFF140601),
        surfaceColor = Color(0xFF260D04),
        cardColor = Color(0xFF3D1607),
        textColor = Color(0xFFFFF7ED),
        textSecondaryColor = Color(0xFFFED7AA),
        primaryContainer = Color(0xFF431407),
        onPrimaryContainer = Color(0xFFFDBA74),
        gradientColors = listOf(Color(0xFFFB923C), Color(0xFFF43F5E)),
        isDark = true
    ),
    OBSIDIAN(
        id = "obsidian",
        titleResId = R.string.theme_obsidian,
        primaryColor = Color(0xFFE2E8F0),
        secondaryColor = Color(0xFF94A3B8),
        tertiaryColor = Color(0xFF38BDF8),
        backgroundColor = Color(0xFF0B0E14),
        surfaceColor = Color(0xFF161B22),
        cardColor = Color(0xFF21262D),
        textColor = Color(0xFFF8FAFC),
        textSecondaryColor = Color(0xFF94A3B8),
        primaryContainer = Color(0xFF1E293B),
        onPrimaryContainer = Color(0xFFF1F5F9),
        gradientColors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8)),
        isDark = true
    ),
    AURORA(
        id = "aurora",
        titleResId = R.string.theme_aurora,
        primaryColor = Color(0xFF2DD4BF),
        secondaryColor = Color(0xFFA78BFA),
        tertiaryColor = Color(0xFF4ADE80),
        backgroundColor = Color(0xFF041017),
        surfaceColor = Color(0xFF0B2533),
        cardColor = Color(0xFF11384D),
        textColor = Color(0xFFF0FDFA),
        textSecondaryColor = Color(0xFF99F6E4),
        primaryContainer = Color(0xFF134E4A),
        onPrimaryContainer = Color(0xFF5EEAD4),
        gradientColors = listOf(Color(0xFF2DD4BF), Color(0xFFA78BFA)),
        isDark = true
    ),
    DARK(
        id = "dark",
        titleResId = R.string.theme_dark,
        primaryColor = Color(0xFF7FDBCA),
        secondaryColor = Color(0xFF9CC5FF),
        tertiaryColor = Color(0xFFFFB59D),
        backgroundColor = Color(0xFF121212),
        surfaceColor = Color(0xFF1E1E1E),
        cardColor = Color(0xFF282828),
        textColor = Color(0xFFF8FAFC),
        textSecondaryColor = Color(0xFF94A3B8),
        primaryContainer = Color(0xFF004F46),
        onPrimaryContainer = Color(0xFF7FDBCA),
        gradientColors = listOf(Color(0xFF7FDBCA), Color(0xFF9CC5FF)),
        isDark = true
    ),

    // ☀️ LIGHT THEMES (Soft tinted atmospheric canvases, cohesive surfaces & vibrant contrast)
    LAVENDER_MIST(
        id = "lavender_mist",
        titleResId = R.string.theme_lavender_mist,
        primaryColor = Color(0xFF7C3AED),
        secondaryColor = Color(0xFFDB2777),
        tertiaryColor = Color(0xFF4F46E5),
        backgroundColor = Color(0xFFFAF5FF),
        surfaceColor = Color(0xFFF3E8FF),
        cardColor = Color(0xFFEDE4F9),
        textColor = Color(0xFF2E1065),
        textSecondaryColor = Color(0xFF6B21A8),
        primaryContainer = Color(0xFFE9D5FF),
        onPrimaryContainer = Color(0xFF581C87),
        gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFDB2777)),
        isDark = false
    ),
    NORDIC_FROST(
        id = "nordic_frost",
        titleResId = R.string.theme_nordic_frost,
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF0D9488),
        tertiaryColor = Color(0xFF6366F1),
        backgroundColor = Color(0xFFF0F9FF),
        surfaceColor = Color(0xFFE0F2FE),
        cardColor = Color(0xFFD6EEFD),
        textColor = Color(0xFF0F172A),
        textSecondaryColor = Color(0xFF334155),
        primaryContainer = Color(0xFFBAE6FD),
        onPrimaryContainer = Color(0xFF0369A1),
        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0D9488)),
        isDark = false
    ),
    MINT_BREEZE(
        id = "mint_breeze",
        titleResId = R.string.theme_mint_breeze,
        primaryColor = Color(0xFF059669),
        secondaryColor = Color(0xFF0284C7),
        tertiaryColor = Color(0xFF16A34A),
        backgroundColor = Color(0xFFF0FDF4),
        surfaceColor = Color(0xFFDCFCE7),
        cardColor = Color(0xFFD1FADF),
        textColor = Color(0xFF064E3B),
        textSecondaryColor = Color(0xFF14532D),
        primaryContainer = Color(0xFFA7F3D0),
        onPrimaryContainer = Color(0xFF065F46),
        gradientColors = listOf(Color(0xFF059669), Color(0xFF0284C7)),
        isDark = false
    ),
    SOLAR_AMBER(
        id = "solar_amber",
        titleResId = R.string.theme_solar_amber,
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFFEA580C),
        tertiaryColor = Color(0xFFDC2626),
        backgroundColor = Color(0xFFFFFBEB),
        surfaceColor = Color(0xFFFEF3C7),
        cardColor = Color(0xFFFDEBB2),
        textColor = Color(0xFF451A03),
        textSecondaryColor = Color(0xFF78350F),
        primaryContainer = Color(0xFFFDE68A),
        onPrimaryContainer = Color(0xFF92400E),
        gradientColors = listOf(Color(0xFFD97706), Color(0xFFEA580C)),
        isDark = false
    ),
    ROSE_SUNSET(
        id = "rose_sunset",
        titleResId = R.string.theme_rose_sunset,
        primaryColor = Color(0xFFE11D48),
        secondaryColor = Color(0xFFF97316),
        tertiaryColor = Color(0xFFBE185D),
        backgroundColor = Color(0xFFFFF1F2),
        surfaceColor = Color(0xFFFFE4E6),
        cardColor = Color(0xFFFED2D8),
        textColor = Color(0xFF4C0519),
        textSecondaryColor = Color(0xFF881337),
        primaryContainer = Color(0xFFFECDD3),
        onPrimaryContainer = Color(0xFF9F1239),
        gradientColors = listOf(Color(0xFFE11D48), Color(0xFFF97316)),
        isDark = false
    ),
    LIGHT(
        id = "light",
        titleResId = R.string.theme_light,
        primaryColor = Color(0xFF006B5E),
        secondaryColor = Color(0xFF3B5E8C),
        tertiaryColor = Color(0xFF9C4325),
        backgroundColor = Color(0xFFF8F9FA),
        surfaceColor = Color(0xFFEEF2F6),
        cardColor = Color(0xFFE2E8F0),
        textColor = Color(0xFF0F172A),
        textSecondaryColor = Color(0xFF475569),
        primaryContainer = Color(0xFFCCE8E3),
        onPrimaryContainer = Color(0xFF003730),
        gradientColors = listOf(Color(0xFF006B5E), Color(0xFF3B5E8C)),
        isDark = false
    );

    companion object {
        val darkThemes = listOf(CYBER_ICE, NEBULA, MIDNIGHT, FOREST, SOLAR, OBSIDIAN, AURORA, DARK)
        val lightThemes = listOf(LAVENDER_MIST, NORDIC_FROST, MINT_BREEZE, SOLAR_AMBER, ROSE_SUNSET, LIGHT)

        fun fromId(id: String?): AppTheme = when (id) {
            "cyber_ice", "arctic", "ocean" -> CYBER_ICE
            "nebula", "royal" -> NEBULA
            "midnight", "electric" -> MIDNIGHT
            "forest" -> FOREST
            "solar", "fire" -> SOLAR
            "obsidian" -> OBSIDIAN
            "sunset", "rose_sunset" -> ROSE_SUNSET
            "aurora" -> AURORA
            "nordic_frost" -> NORDIC_FROST
            "mint_breeze" -> MINT_BREEZE
            "solar_amber" -> SOLAR_AMBER
            "lavender_mist" -> LAVENDER_MIST
            "light" -> LIGHT
            "dark" -> DARK
            "system" -> SYSTEM
            else -> entries.find { it.id == id } ?: SYSTEM
        }
    }
}
