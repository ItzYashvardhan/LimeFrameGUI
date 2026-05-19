package net.justlime.limeframegui.models.registry

import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiItem

data class DynamicListMask(
    val populatorId: String,
    val slots: List<Int>,
    val templateItem: GuiItem,
    val buffer: GuiBuffer
)