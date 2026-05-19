package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection
import java.lang.StringBuilder
import kotlin.text.iterator

object FontRegistry : IRegistry {
    private val fontsByVersion = mutableMapOf<String, Map<Char, Char>>()
    val getFonts: Map<String, Map<Char, Char>> get() = fontsByVersion
    override fun load(section: ConfigurationSection) {
        fontsByVersion.clear()

        for (versionKey in section.getKeys(false)) {
            val versionSection = section.getConfigurationSection(versionKey) ?: continue
            val charMap = mutableMapOf<Char, Char>()

            for (charKey in versionSection.getKeys(false)) {
                val mappedValue = versionSection.getString(charKey)
                if (charKey.isNotEmpty() && !mappedValue.isNullOrEmpty()) {
                    // Treat keys as lowercase characters since config is not case sensitive
                    charMap[charKey.lowercase()[0]] = mappedValue[0]
                }
            }
            fontsByVersion[versionKey] = charMap
        }
    }

    override fun append(section: ConfigurationSection) {
        for (versionKey in section.getKeys(false)) {
            val versionSection = section.getConfigurationSection(versionKey) ?: continue
            val charMap = fontsByVersion.getOrPut(versionKey) { mutableMapOf() }.toMutableMap()

            for (charKey in versionSection.getKeys(false)) {
                val mappedValue = versionSection.getString(charKey)
                if (charKey.isNotEmpty() && !mappedValue.isNullOrEmpty()) {
                    charMap[charKey.lowercase()[0]] = mappedValue[0]
                }
            }
            fontsByVersion[versionKey] = charMap
        }
    }

    override fun clear() {
        fontsByVersion.clear()
    }

    /**
     * Translates a string using the font map for a specific version.
     * If a version is not provided or not found, it returns the original string.
     */
    fun translate(text: String, version: String? = null): String {
        if (version == null || !fontsByVersion.containsKey(version)) {
            return text
        }

        val charMap = fontsByVersion[version] ?: return text
        val builder = StringBuilder(text.length)

        for (char in text) {
            val lowerChar = char.lowercaseChar()
            if (charMap.containsKey(lowerChar)) {
                // If the original was uppercase, you might want to map to uppercase if your custom font supports it, 
                // but the prompt states case insensitive, so we just use the mapped char directly.
                builder.append(charMap[lowerChar])
            } else {
                builder.append(char)
            }
        }
        return builder.toString()
    }
}
