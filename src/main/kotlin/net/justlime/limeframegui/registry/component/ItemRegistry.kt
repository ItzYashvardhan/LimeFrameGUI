package net.justlime.limeframegui.registry.component

import net.justlime.limeframegui.config.GuiConfigHandler
import net.justlime.limeframegui.models.GuiItem
import org.bukkit.configuration.ConfigurationSection

object ItemRegistry : IRegistry {
    private val globalPalette = mutableMapOf<Char, GuiItem>()
    private val globalItemsById = mutableMapOf<String, GuiItem>()

    fun getByChar(char: Char): GuiItem? = globalPalette[char]
    fun getById(id: String): GuiItem? = globalItemsById[id]

    override fun load(section: ConfigurationSection) {
        globalPalette.clear()
        globalItemsById.clear()
        append(section)
    }

    override fun append(section: ConfigurationSection) {
        for (key in section.getKeys(false)) {
            val itemSection = section.getConfigurationSection(key) ?: continue
            val item = GuiConfigHandler.loadItem(itemSection)
            
            globalItemsById[key] = item
            
            val charStr = itemSection.getString("char")
            if (!charStr.isNullOrEmpty()) {
                globalPalette[charStr.first()] = item
            }
        }
    }

    override fun clear() {
        globalPalette.clear()
        globalItemsById.clear()
    }
}