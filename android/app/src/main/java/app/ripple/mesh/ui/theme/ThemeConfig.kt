package app.ripple.mesh.ui.theme

import androidx.compose.ui.graphics.Color
import app.ripple.mesh.R

enum class AppTheme(
    val id: String,
    val titleResId: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val cardColor: Color,
    val textColor: Color,
    val isDark: Boolean
) {
    SYSTEM(
        "system", R.string.theme_system,
        Color(0xFF006B5E), Color(0xFF3B5E8C),
        Color(0xFFF8F9FA), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF0F172A),
        false
    ),
    LIGHT(
        "light", R.string.theme_light,
        Color(0xFF006B5E), Color(0xFF3B5E8C),
        Color(0xFFF8F9FA), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF0F172A),
        false
    ),
    DARK(
        "dark", R.string.theme_dark,
        Color(0xFF7FDBCA), Color(0xFF9CC5FF),
        Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF252525), Color(0xFFF8FAFC),
        true
    ),
    OCEAN(
        "ocean", R.string.theme_ocean,
        Color(0xFF0EA5A4), Color(0xFF0284C7),
        Color(0xFF071A2B), Color(0xFF064E5B), Color(0xFF0F2942), Color(0xFFF8FAFC),
        true
    ),
    NEBULA(
        "nebula", R.string.theme_nebula,
        Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF070B1F), Color(0xFF21104A), Color(0xFF131836), Color(0xFFF8FAFC),
        true
    ),
    FIRE(
        "fire", R.string.theme_fire,
        Color(0xFFF97316), Color(0xFFEF4444),
        Color(0xFF170504), Color(0xFF571313), Color(0xFF2C1210), Color(0xFFF8FAFC),
        true
    ),
    ARCTIC(
        "arctic", R.string.theme_arctic,
        Color(0xFF06B6D4), Color(0xFF3B82F6),
        Color(0xFF031525), Color(0xFF075985), Color(0xFF0A233A), Color(0xFFF8FAFC),
        true
    ),
    MIDNIGHT(
        "midnight", R.string.theme_midnight,
        Color(0xFF3B82F6), Color(0xFF6366F1),
        Color(0xFF020617), Color(0xFF0F172A), Color(0xFF111827), Color(0xFFF8FAFC),
        true
    ),
    FOREST(
        "forest", R.string.theme_forest,
        Color(0xFF10B981), Color(0xFF16A34A),
        Color(0xFF03140D), Color(0xFF064E3B), Color(0xFF0A231C), Color(0xFFF8FAFC),
        true
    ),
    ELECTRIC(
        "electric", R.string.theme_electric,
        Color(0xFF2563EB), Color(0xFF06B6D4),
        Color(0xFF050817), Color(0xFF172554), Color(0xFF0B132B), Color(0xFFF8FAFC),
        true
    ),
    ROYAL(
        "royal", R.string.theme_royal,
        Color(0xFF9333EA), Color(0xFFC026D3),
        Color(0xFF0D0618), Color(0xFF3B0764), Color(0xFF1F0B38), Color(0xFFF8FAFC),
        true
    ),
    OBSIDIAN(
        "obsidian", R.string.theme_obsidian,
        Color(0xFFE5E5E5), Color(0xFF737373),
        Color(0xFF000000), Color(0xFF111111), Color(0xFF171717), Color(0xFFFAFAFA),
        true
    ),
    SUNSET(
        "sunset", R.string.theme_sunset,
        Color(0xFFF43F5E), Color(0xFFF97316),
        Color(0xFF180B2B), Color(0xFF831843), Color(0xFF2D1223), Color(0xFFF8FAFC),
        true
    ),
    AURORA(
        "aurora", R.string.theme_aurora,
        Color(0xFF14B8A6), Color(0xFF8B5CF6),
        Color(0xFF04121A), Color(0xFF064E5B), Color(0xFF0A2330), Color(0xFFF8FAFC),
        true
    ),
    CYBER_ICE(
        "cyber_ice", R.string.theme_cyber_ice,
        Color(0xFF22D3EE), Color(0xFF6366F1),
        Color(0xFF020617), Color(0xFF0C4A6E), Color(0xFF0B1B32), Color(0xFFF8FAFC),
        true
    ),
    SOLAR(
        "solar", R.string.theme_solar,
        Color(0xFFF59E0B), Color(0xFFEA580C),
        Color(0xFF160B02), Color(0xFF78350F), Color(0xFF2B1A0A), Color(0xFFF8FAFC),
        true
    );

    companion object {
        fun fromId(id: String?): AppTheme = entries.find { it.id == id } ?: SYSTEM
    }
}
