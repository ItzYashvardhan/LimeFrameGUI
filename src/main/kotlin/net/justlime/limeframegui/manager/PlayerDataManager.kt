package net.justlime.limeframegui.manager

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object PlayerDataManager {
    private val playerLangs = mutableMapOf<String, String>()
    private lateinit var dataFile: File
    private lateinit var config: YamlConfiguration

    fun init(plugin: JavaPlugin,path: String) {
        val dataFolder = File(plugin.dataFolder, path)
        if (!dataFolder.exists()) dataFolder.mkdirs()

        dataFile = File(dataFolder, "player_lang.yml")
        if (!dataFile.exists()) dataFile.createNewFile()

        config = YamlConfiguration.loadConfiguration(dataFile)
        
        // Load into fast memory Map
        for (uuid in config.getKeys(false)) {
            playerLangs[uuid] = config.getString(uuid) ?: "en_us"
        }
    }

    /**
     * Gets a player's selected language. 
     * Falls back to their Minecraft Client locale, then falls back to English.
     */
    fun getPlayerLocale(player: Player): String {
        val uuid = player.uniqueId.toString()
        // 1. Check if they picked a custom language in our data file
        if (playerLangs.containsKey(uuid)) {
            return playerLangs[uuid]!!
        }


        // 2. Otherwise, read their literal Minecraft client setting
        return player.locale.lowercase() 
    }

    /**
     * Changes a player's language and saves it to the disk.
     */
    fun setPlayerLocale(player: Player, localeCode: String) {
        val uuid = player.uniqueId.toString()
        playerLangs[uuid] = localeCode
        config.set(uuid, localeCode)
        config.save(dataFile)
    }
}