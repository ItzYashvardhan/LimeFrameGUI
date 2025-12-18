package net.justlime.limeframegui.models

import org.bukkit.event.inventory.InventoryType

data class GUISetting(
    var rows: Int,//
    var title: String,//
    var style: GuiStyleSheet = GuiStyleSheet(),//
    var type: InventoryType = InventoryType.CHEST,//
) {
    fun clone(): GUISetting {
        return GUISetting(rows, title, style.copy(), type)
    }
}