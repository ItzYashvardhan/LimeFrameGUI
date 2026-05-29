package net.justlime.limeframegui.models.response

import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.models.registry.DynamicListMask
import org.bukkit.entity.Player

data class ListPopulatorResponse(
    val player: Player,
    val mask: DynamicListMask,
    val setting: GuiSetting
)