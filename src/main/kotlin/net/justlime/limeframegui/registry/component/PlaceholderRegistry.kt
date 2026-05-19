package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection

/**
 * Global pre-processor for YAML placeholders.
 * Resolves static placeholders (e.g., {cmd}, {snd}) at load-time to prevent runtime overhead.
 * Configurable prefix and suffix prevent conflicts with PlaceholderAPI (%value%).
 */
object PlaceholderRegistry : IRegistry {
    private val placeholders = mutableMapOf<String, String>()

    // Customizable wrapper symbols. Defaults to {value}
    var prefix: String = "{"
    var suffix: String = "}"

    override fun load(section: ConfigurationSection) {
        placeholders.clear()
        append(section)
    }

    override fun append(section: ConfigurationSection) {
        for (key in section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                placeholders[key] = section.getString(key) ?: ""
            }
        }
    }

    override fun clear() {
        placeholders.clear()
    }

    /**
     * Scans a string and replaces all known placeholders wrapped in the configured symbols.
     */
    fun resolve(text: String): String {
        if (!text.contains(prefix)) return text // Fast exit for performance
        
        var resolved = text
        for ((key, value) in placeholders) {
            val target = "$prefix$key$suffix"
            if (resolved.contains(target)) {
                resolved = resolved.replace(target, value)
            }
        }
        return resolved
    }

    /**
     * Resolves an entire list of strings.
     */
    fun resolve(list: List<String>): List<String> {
        if (list.isEmpty()) return list
        return list.map { resolve(it) }
    }
}