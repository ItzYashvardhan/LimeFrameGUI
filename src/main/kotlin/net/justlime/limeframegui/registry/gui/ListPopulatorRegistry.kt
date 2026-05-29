package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.models.registry.DynamicListMask
import net.justlime.limeframegui.models.response.ListPopulatorResponse
import org.bukkit.entity.Player
import sun.audio.AudioPlayer.player

object ListPopulatorRegistry {

    private val populators = mutableMapOf<String, (response: ListPopulatorResponse) -> List<GuiItem>>()

    fun register(id: String, provider: (response: ListPopulatorResponse) -> List<GuiItem>) {
        populators[id] = provider
    }

    fun getItems(id: String, response: ListPopulatorResponse): List<GuiItem> {
        return populators[id]?.invoke(response) ?: emptyList()
    }
}