package net.justlime.limeframegui.registry.component

import net.justlime.limeframegui.models.registry.GuiSound
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection

object SoundRegistry : IRegistry {
    private val rawSounds = mutableMapOf<String, List<String>>()
    private val compiledSounds = mutableMapOf<String, List<GuiSound>>()

    fun get(key: String): List<GuiSound> {
        return compiledSounds[key] ?: emptyList()
    }

    override fun load(section: ConfigurationSection) {
        rawSounds.clear()
        compiledSounds.clear()

        // Pass 1: Flatten YAML to RawMap
        flattenToRawMap(section, "", rawSounds)

        // Pass 2: Resolve All Raw Entries
        for (key in rawSounds.keys) {
            compiledSounds[key] = resolveSound(key)
        }
    }

    override fun append(section: ConfigurationSection) {
        val newRawSounds = mutableMapOf<String, List<String>>()
        flattenToRawMap(section, "", newRawSounds)

        // Merge new raw sounds into existing ones
        rawSounds.putAll(newRawSounds)

        // Re-resolve only the keys that were added or affected
        for (key in newRawSounds.keys) {
            compiledSounds[key] = resolveSound(key)
        }
    }

    override fun clear() {
        rawSounds.clear()
        compiledSounds.clear()
    }

    private fun flattenToRawMap(
        section: ConfigurationSection,
        pathPrefix: String,
        map: MutableMap<String, List<String>>
    ) {
        for (key in section.getKeys(false)) {
            val currentPath = if (pathPrefix.isEmpty()) key else "$pathPrefix.$key"

            if (section.isConfigurationSection(key)) {
                flattenToRawMap(section.getConfigurationSection(key)!!, currentPath, map)
            } else {
                if (section.isList(key)) {
                    map[currentPath] = section.getStringList(key)
                } else {
                    map[currentPath] = listOf(section.getString(key) ?: "")
                }
            }
        }
    }

    private fun resolveSound(
        alias: String,
        inheritedPitch: Float? = null,
        inheritedVolume: Float? = null,
        inheritedDelay: Long = 0,
        visited: MutableSet<String> = mutableSetOf()
    ): List<GuiSound> {
        val result = mutableListOf<GuiSound>()

        // Split the instruction to separate the alias/sound from suffixes
        val parts = alias.split(",").map { it.trim() }
        val rawBaseNames = parts[0] // e.g., "my-sound + my-sound2"

        // Parse suffixes for this specific call
        var localPitch = inheritedPitch ?: 1.0f
        var localVolume = inheritedVolume ?: 1.0f
        var localDelay = inheritedDelay

        for (i in 1 until parts.size) {
            val part = parts[i]
            when {
                part.endsWith("p", ignoreCase = true) -> localPitch = part.dropLast(1).toFloatOrNull() ?: localPitch
                part.endsWith("v", ignoreCase = true) -> localVolume = part.dropLast(1).toFloatOrNull() ?: localVolume
                part.endsWith("d", ignoreCase = true) -> localDelay += part.dropLast(1).toLongOrNull() ?: 0L
                else -> {
                    // Fallback for no-suffix
                    when (i) {
                        1 -> localPitch = part.toFloatOrNull() ?: localPitch
                        2 -> localVolume = part.toFloatOrNull() ?: localVolume
                        3 -> localDelay += part.toLongOrNull() ?: 0L
                    }
                }
            }
        }

        // 🌟 THE MAGIC COMBINATOR: Split by '+' to process multiple sounds on one line!
        val baseNames = rawBaseNames.split("+").map { it.trim() }

        for (baseName in baseNames) {
            if (baseName.isEmpty()) continue

            // 1. Is it a Bukkit Sound Enum? (Automatically fixes dots to underscores!)
            val enumValue = try {
                Sound.valueOf(baseName.uppercase().replace(".", "_"))
            } catch (e: Exception) {
                null
            }

            if (enumValue != null) {
                result.add(GuiSound(enumValue.name(), localPitch, localVolume, localDelay))
                continue // Successfully parsed enum, move to the next sound in the combo
            }

            if (!rawSounds.containsKey(baseName)) {
                println("[LimeFrameGUI] Warning: Sound or Alias '$baseName' not found in registry.")
                continue // Skip invalid sound
            }

            // 2. Is it a raw alias?
            if (visited.contains(baseName)) {
                println("[LimeFrameGUI] Warning: Infinite sound loop detected at '$baseName'! Breaking cycle.")
                continue
            }

            visited.add(baseName)
            val subInstructions = rawSounds[baseName] ?: emptyList()
            for (instruction in subInstructions) {
                // Pass down the COMBINED localDelay to everything inside the alias
                result.addAll(resolveSound(instruction, localPitch, localVolume, localDelay, visited))
            }
            visited.remove(baseName)
        }

        return result
    }
}
