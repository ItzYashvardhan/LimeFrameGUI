package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import sun.audio.AudioPlayer.player

/**
 * Pre-processor for YAML placeholders.
 * Resolves static placeholders with priority: Local Override -> Code Injected -> Global Registration.
 */
object PlaceholderRegistry : IRegistry {

    // Holds placeholders loaded from YAML files (Cleared on reload)
    private val configPlaceholders = mutableMapOf<String, String>()

    // Holds placeholders injected dynamically via code (Survives reloads)
    private val injectedPlaceholders = mutableMapOf<String, (Player) -> String>()

    var prefix: String = "{"
    var suffix: String = "}"

    override fun load(section: ConfigurationSection) {
        configPlaceholders.clear()
        append(section)
    }

    override fun append(section: ConfigurationSection) {
        for (key in section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                configPlaceholders[key] = section.getString(key) ?: ""
            }
        }
    }

    /**
     * Registers a custom placeholder via code.
     * These are stored separately and will survive a plugin reload.
     */
    fun register(key: String, resolver: (Player) -> String) {
        injectedPlaceholders[key] = resolver
    }

    /**
     * Clears ONLY the config-based placeholders.
     * This allows your reload() function to run safely without wiping code injections.
     */
    override fun clear() {
        configPlaceholders.clear()
    }

    /**
     * Optional: A method to clear everything if you ever need a true hard reset.
     */
    fun clearAll() {
        configPlaceholders.clear()
        injectedPlaceholders.clear()
    }

    /**
     * Resolves placeholders, prioritizing Locals -> Injected -> Config.
     */
    fun resolve(text: String, localOverrides: Map<String, String> = emptyMap(),player: Player? = null): String {
        if (!text.contains(prefix)) return text

        var resolved = text

        // Priority 1: Process Local Overrides (Highest Priority)
        for ((key, value) in localOverrides) {
            val target = "$prefix$key$suffix"
            if (resolved.contains(target)) {
                resolved = resolved.replace(target, value)
            }
        }

        // Priority 2: Process Code-Injected Registrations (Survives reloads)
        for ((key, resolverFunction) in injectedPlaceholders) {
            val target = "$prefix$key$suffix"
            if (resolved.contains(target)) {
                // Execute the lambda to get the live value for this specific player
                if (player != null){
                    resolved = resolved.replace(target, resolverFunction(player))
                }
            }
        }

        // Priority 3: Process Global Config Registrations (Lowest Priority, from YAML)
        for ((key, value) in configPlaceholders) {
            val target = "$prefix$key$suffix"
            if (resolved.contains(target)) {
                resolved = resolved.replace(target, value)
            }
        }

        return resolved
    }

    /**
     * Resolves an entire list of strings with local overrides.
     */
    fun resolve(list: List<String>, localOverrides: Map<String, String> = emptyMap()): List<String> {
        if (list.isEmpty()) return list
        return list.map { resolve(it, localOverrides) }
    }
}