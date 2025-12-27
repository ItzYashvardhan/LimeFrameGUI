package net.justlime.limeframegui.listener

import net.justlime.limeframegui.handler.GuiEventHandler
import net.justlime.limeframegui.integration.SkinRestorerHook
import net.justlime.limeframegui.utilities.FrameAdapter
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent

class PluginListener : Listener {
    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        val onlinePlayers = event.plugin.server.onlinePlayers
        for (player in onlinePlayers) {
            val openInventory = FrameAdapter.getTopInventorySafe(player)
            if (openInventory?.holder is GuiEventHandler) {
                player.closeInventory()
            }
        }
        SkinRestorerHook.close()
    }
}