package app.ripple.mesh.ui.theme

import app.ripple.mesh.R

enum class AppTheme(val id: String, val titleResId: Int) {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark);

    companion object {
        fun fromId(id: String?): AppTheme = entries.find { it.id == id } ?: SYSTEM
    }
}
