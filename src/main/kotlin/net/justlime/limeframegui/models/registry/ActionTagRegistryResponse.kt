package net.justlime.limeframegui.models.registry

import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.models.GuiItem
import org.bukkit.entity.Player

data class ActionTagRegistryResponse(
    val player: Player,
    val payload: String,
    val handler: GuiEventHandler?,
    val context: IContextSetting?,
    val item: GuiItem? = null
    )
