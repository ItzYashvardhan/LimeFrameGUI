package net.justlime.limeframegui.models

import org.bukkit.event.inventory.InventoryType

data class GUISetting(
    var rows: Int,
    var title: String,
    var styleSheet: LimeStyleSheet? = null,
    var type: InventoryType = InventoryType.CHEST,
) {
    fun clone(): GUISetting {
        return GUISetting(rows, title, styleSheet?.copy(), type)
    }
}