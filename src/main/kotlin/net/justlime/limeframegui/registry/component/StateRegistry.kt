package net.justlime.limeframegui.registry.component

import net.justlime.limeframegui.config.GuiConfigHandler
import net.justlime.limeframegui.models.GuiItem
import org.bukkit.configuration.ConfigurationSection

object StateRegistry : IRegistry {
    private val stateComponents = mutableMapOf<String, GuiItem>()

    /**
     * Fetches a shared state component by its key.
     */
    fun get(key: String): GuiItem? {
        return stateComponents[key]?.clone()
    }

    override fun load(section: ConfigurationSection) {
        stateComponents.clear()
        append(section)
    }

    override fun append(section: ConfigurationSection) {
        for (key in section.getKeys(false)) {
            val itemSection = section.getConfigurationSection(key) ?: continue
            val component = GuiConfigHandler.loadItem(itemSection)
            stateComponents[key] = component
        }
    }

    override fun clear() {
        stateComponents.clear()
    }
}