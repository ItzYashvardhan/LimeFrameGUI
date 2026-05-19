package net.justlime.limeframegui.registry.gui

import org.bukkit.configuration.file.YamlConfiguration

object TemplateRegistry {
    private val templates = mutableMapOf<String, YamlConfiguration>()

    fun register(id: String, config: YamlConfiguration) {
        templates[id] = config
    }

    fun get(id: String): YamlConfiguration? {
        return templates[id]
    }

    fun clear() {
        templates.clear()
    }
}