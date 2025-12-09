package net.justlime.limeframegui

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.enums.ColorType
import net.justlime.limeframegui.example.commands.CommandManager
import org.bukkit.plugin.java.JavaPlugin

/** Using it for just testing*/
class LimeFrameGUI : JavaPlugin() {

    override fun onEnable() {
        this.saveDefaultConfig()
        CommandManager(this)
        LimeFrameAPI.init(this, ColorType.MINI_MESSAGE)
        LimeFrameAPI.setKeys {
            stylishTitle = true;
            stylishName = true;
            stylishLore = false
        }
        LimeFrameAPI.debugging = true
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

}
