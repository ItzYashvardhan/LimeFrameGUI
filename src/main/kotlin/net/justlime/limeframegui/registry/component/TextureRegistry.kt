package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection

object TextureRegistry : IRegistry {
    private val textures = mutableMapOf<String, String>()

    fun get(key: String): String? {
        return textures[key]
    }

    override fun load(section: ConfigurationSection) {
        textures.clear()
        flattenSection(section, "", textures)
    }

    override fun append(section: ConfigurationSection) {
        flattenSection(section, "", textures)
    }

    override fun clear() {
        textures.clear()
    }

    private fun flattenSection(section: ConfigurationSection, pathPrefix: String, map: MutableMap<String, String>) {
        for (key in section.getKeys(false)) {
            val currentPath = if (pathPrefix.isEmpty()) key else "$pathPrefix.$key"

            if (section.isConfigurationSection(key)) {
                flattenSection(section.getConfigurationSection(key)!!, currentPath, map)
            } else {
                map[currentPath] = section.getString(key) ?: ""
            }
        }
    }
}
