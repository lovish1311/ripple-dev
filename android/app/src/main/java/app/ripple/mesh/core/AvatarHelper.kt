package app.ripple.mesh.core

/**
 * Robust helper for formatting and extracting avatar emojis and clean display names.
 * Ensures consistent peer identity rendering across BLE mesh announcements,
 * Room persistence, and UI components.
 */
object AvatarHelper {
    const val DEFAULT_AVATAR = "🧑‍🚀"

    val PRESETS = listOf(
        "👦", "👧", "👨‍🚀", "👩‍💻", "🧑‍🎤", "🧔", "👩‍⚕️", "🕵️",
        "🐱", "🐶", "🦊", "🐼", "🦁", "🐺", "🐰", "🐻",
        "🦅", "🦉", "🐲", "🐬", "🦈", "🐙", "⚡", "🔥"
    )

    /**
     * Formats wire display name for mesh announcement.
     * E.g. avatar="👨‍🚀", name="Rip" -> "👨‍🚀 Rip"
     */
    fun formatWireName(avatar: String?, name: String): String {
        val trimmedName = name.trim()
        val av = avatar?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_AVATAR
        val (_, cleanName) = extractAvatarAndName(trimmedName)
        val finalClean = if (cleanName.isNotBlank()) cleanName else trimmedName
        return "$av $finalClean".trim()
    }

    /**
     * Extracts avatar emoji and clean display name from a raw wire name string.
     * E.g. "👨‍🚀 Rip" -> ("👨‍🚀", "Rip")
     *      "🦊 Tablet" -> ("🦊", "Tablet")
     *      "Rip" -> (null, "Rip")
     */
    fun extractAvatarAndName(raw: String): Pair<String?, String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Pair(null, "")

        // Check if delimiter \u001F is used
        if (trimmed.contains("\u001F")) {
            val parts = trimmed.split("\u001F", limit = 2)
            val av = parts[0].trim().takeIf { it.isNotEmpty() }
            val nm = parts.getOrNull(1)?.trim().orEmpty()
            return Pair(av, nm.ifEmpty { av ?: "" })
        }

        // Check preset avatar matching prefix
        for (preset in PRESETS) {
            if (trimmed.startsWith(preset)) {
                val rest = trimmed.substring(preset.length).trim()
                return Pair(preset, rest.ifEmpty { preset })
            }
        }

        // Generic emoji token detection
        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val firstToken = parts[0]
        if (isEmojiToken(firstToken)) {
            val rest = if (parts.size > 1) parts[1].trim() else ""
            return Pair(firstToken, rest.ifEmpty { firstToken })
        }

        return Pair(null, trimmed)
    }

    private fun isEmojiToken(token: String): Boolean {
        if (token.isEmpty()) return false
        var i = 0
        var hasEmojiCp = false
        while (i < token.length) {
            val cp = token.codePointAt(i)
            val isEmojiCp = (cp in 0x1F300..0x1F9FF) ||
                            (cp in 0x1FA00..0x1FAFF) ||
                            (cp in 0x2600..0x27BF) ||
                            (cp in 0x1F600..0x1F64F) ||
                            (cp in 0x1F680..0x1F6FF) ||
                            (cp in 0x2B50..0x2B55) ||
                            (cp == 0x200D) || // ZWJ
                            (cp == 0xFE0F)    // Variation selector
            if (isEmojiCp) hasEmojiCp = true
            i += Character.charCount(cp)
        }
        return hasEmojiCp
    }
}
