package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection

object FontRegistry : IRegistry {

    private val fontsByVersion = mutableMapOf<String, Map<Char, String>>()
    val getFonts: Map<String, Map<Char, String>> get() = fontsByVersion

    override fun load(section: ConfigurationSection) {
        fontsByVersion.clear()

        for (versionKey in section.getKeys(false)) {
            val versionSection = section.getConfigurationSection(versionKey) ?: continue
            val charMap = mutableMapOf<Char, String>()

            for (charKey in versionSection.getKeys(false)) {
                val mappedValue = versionSection.getString(charKey)
                if (charKey.isNotEmpty() && !mappedValue.isNullOrEmpty()) {

                    // 🌟 FIX 2: Save the ENTIRE mappedValue string, DO NOT slice it with [0]!
                    charMap[charKey.lowercase()[0]] = mappedValue
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

                    // 🌟 FIX 2: Save the ENTIRE mappedValue string here too!
                    charMap[charKey.lowercase()[0]] = mappedValue
                }
            }
            fontsByVersion[versionKey] = charMap
        }
    }

    override fun clear() {
        fontsByVersion.clear()
    }
}