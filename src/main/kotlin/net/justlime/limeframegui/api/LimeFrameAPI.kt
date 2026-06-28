package net.justlime.limeframegui.api

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.config.GuiDirectoryHandler
import net.justlime.limeframegui.enums.ColorType
import net.justlime.limeframegui.integration.FoliaLibHook
import net.justlime.limeframegui.integration.SkinRestorerHook
import net.justlime.limeframegui.listener.InventoryListener
import net.justlime.limeframegui.listener.PluginListener
import net.justlime.limeframegui.manager.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * Initialize the Listeners
 * */
object LimeFrameAPI {
    private lateinit var plugin: JavaPlugin
    var debugging: Boolean = false

    fun init(plugin: JavaPlugin, colorType: ColorType = ColorType.LEGACY): LimeFrameAPI {
        this.plugin = plugin
        Bukkit.getPluginManager().registerEvents(InventoryListener(plugin), plugin)

        FontStyle.setColorType(colorType)
        if (colorType == ColorType.MINI_MESSAGE) FontStyle.initMiniMessage()

        SkinRestorerHook.init()

        //run a task on plugin disable
        Bukkit.getPluginManager().registerEvents(PluginListener(), plugin)
        return this
    }

    fun loadConfig(path: String = "gui"): LimeFrameAPI {
        GuiDirectoryHandler.loadAll(plugin, path)
        return this
    }

    fun initLocale(path: String): LimeFrameAPI{
        PlayerDataManager.init(plugin,"gui/data")
        return this
    }


    fun enableFoliaLib(): LimeFrameAPI {
        FoliaLibHook.init(plugin)
        return this
    }

    fun getPlugin(): JavaPlugin = plugin
}