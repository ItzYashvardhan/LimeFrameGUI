package net.justlime.limeframegui

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.enums.ColorType
import net.justlime.limeframegui.example.commands.CommandManager
import net.justlime.limeframegui.registry.component.ButtonRegistry
import org.bukkit.plugin.java.JavaPlugin

/** Using it for just testing*/
class LimeFrameGUI : JavaPlugin() {

    override fun onEnable() {
        this.saveDefaultConfig()
        CommandManager(this)
        LimeFrameAPI.init(this, ColorType.MINI_MESSAGE)
            .setKeys {
                stylishTitle = true
                stylishName = true
                stylishLore = false
                material = "item"
            }
            .loadConfig()
        LimeFrameAPI.debugging = true

        registerMyActions()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    fun registerMyActions() {

        // 1. Registering your Team Wipe Logic
        ButtonRegistry.register("wipe_team_admin_logic") { player, gui ->
            player.sendMessage("§a[System] Initiating deep database wipe...")

            // You can even interact with the GUI from here!
            gui.open(player, 2) // Send them to a confirmation page
        }

        // 2. Registering a Stats Fetcher
        ButtonRegistry.register("fetch_stats") { player, gui ->
            val kills = 150 // Fetch from database
            player.sendMessage("§bYou have $kills kills!")
            player.closeInventory()
        }
    }

}
