package net.justlime.limeframegui.integration

import com.tcoded.folialib.FoliaLib
import org.bukkit.plugin.java.JavaPlugin

object FoliaLibHook {
    lateinit var foliaLib: FoliaLib

    fun init(plugin: JavaPlugin) {
        foliaLib = FoliaLib(plugin)
    }

}