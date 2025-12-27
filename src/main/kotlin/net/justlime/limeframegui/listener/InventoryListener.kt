package net.justlime.limeframegui.listener

import net.justlime.limeframegui.handler.GuiEventHandler
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.plugin.java.JavaPlugin

class InventoryListener(val plugin: JavaPlugin) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val upperInventory = event.inventory
        val holder = upperInventory.holder
        val playerInventory = event.whoClicked.inventory
        val clickedInventory = event.clickedInventory
        if (playerInventory == clickedInventory) return
        if (holder !is GuiEventHandler) return
        holder.onEvent(event)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val upperInventory = event.inventory
        val holder = upperInventory.holder
        if (holder !is GuiEventHandler) return
        holder.onEvent(event, plugin)
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val upperInventory = event.inventory
        val holder = upperInventory.holder
        if (holder !is GuiEventHandler) return
        holder.onEvent(event)
    }
}
