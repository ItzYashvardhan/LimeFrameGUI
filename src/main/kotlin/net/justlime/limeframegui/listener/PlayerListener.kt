package net.justlime.limeframegui.listener

import net.justlime.limeframegui.manager.SessionManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener: Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        SessionManager.loadSession(event.player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        SessionManager.unloadSession(event.player.uniqueId)
    }
}