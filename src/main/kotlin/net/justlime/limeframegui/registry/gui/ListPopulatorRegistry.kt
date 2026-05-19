package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.models.GuiItem
import org.bukkit.entity.Player

object ListPopulatorRegistry {
    // Maps ID -> Function that takes (Player, TemplateItem) and returns a list of fully formatted GuiItems
    private val populators = mutableMapOf<String, (Player, GuiItem) -> List<GuiItem>>()

    fun register(id: String, provider: (Player, GuiItem) -> List<GuiItem>) {
        populators[id] = provider
    }

    fun getItems(id: String, player: Player, templateItem: GuiItem): List<GuiItem> {
        return populators[id]?.invoke(player, templateItem) ?: emptyList()
    }
}