package net.justlime.limeframegui.config

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class YamlFileHandler(private val file: File) {

    var config: YamlConfiguration = loadYaml()
        private set

    constructor(dataFolder: File, fileName: String) : this(File(dataFolder, fileName))

    fun reload(): Boolean {
        return if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file)
            true
        } else false
    }

    fun save(): Boolean {
        return runCatching { config.save(file) }.isSuccess
    }

    private fun loadYaml(): YamlConfiguration {
        val yamlConfig = YamlConfiguration()
        if (!file.exists()) {
            val location = file.parentFile?.absolutePath ?: file.absolutePath
            Bukkit.getLogger().warning("[LimeFrameGUI] File ${file.name} does not exist in $location")
            return yamlConfig
        }
        yamlConfig.load(file)
        return yamlConfig
    }
}