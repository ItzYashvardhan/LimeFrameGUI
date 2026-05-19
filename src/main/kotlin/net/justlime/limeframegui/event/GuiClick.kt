package net.justlime.limeframegui.event

import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.util.setItem
import org.bukkit.event.inventory.InventoryClickEvent

class GuiClick(val event: InventoryClickEvent) {
    fun addItem(item: GuiItem, onClick: GuiClick.(InventoryClickEvent) -> Unit = {}){
        val inventory = event.view.topInventory
        val slot = inventory.firstEmpty()
        if (slot != -1) {
            inventory.setItem(slot, item)
        }
    }
}

