package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.registry.DynamicListMask
import org.bukkit.entity.Player

object ListPopulatorRegistry {

    private val populators = mutableMapOf<String, (Player, DynamicListMask) -> List<GuiItem>>()

    fun register(id: String, provider: (Player, DynamicListMask) -> List<GuiItem>) {
        populators[id] = provider
    }

    fun getItems(id: String, player: Player, mask: DynamicListMask): List<GuiItem> {
        return populators[id]?.invoke(player, mask) ?: emptyList()
    }
}