package net.justlime.limeframegui.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

object FrameAdapter {
    fun getTopInventorySafe(player: Player): Inventory? {
        return try {
            //Get Open Inventory Method
            val getOpenInvMethod = player.javaClass.getMethod("getOpenInventory")
            val inventoryView = getOpenInvMethod.invoke(player)

            val getTopInvMethod = inventoryView.javaClass.getDeclaredMethod("getTopInventory")

            // bypass package-private restriction
            getTopInvMethod.isAccessible = true

            getTopInvMethod.invoke(inventoryView) as? Inventory
        } catch (ex: Throwable) {
            Bukkit.getLogger().warning("Failed to get top inventory: ${ex.message}")
            null
        }
    }
}