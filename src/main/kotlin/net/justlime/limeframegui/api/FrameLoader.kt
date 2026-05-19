package net.justlime.limeframegui.api

import net.justlime.limeframegui.registry.component.LangRegistry
import net.justlime.limeframegui.registry.component.SoundRegistry
import net.justlime.limeframegui.registry.component.TextureRegistry
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object FrameLoader {

    /**
     * Loads a custom Sound config from an external plugin and appends it to the SoundRegistry.
     * * @param plugin The developer's JavaPlugin instance.
     * @param filePath The path inside their plugin folder (e.g., "limegui/lime_sound.yml").
     */
    fun loadCustomSounds(plugin: JavaPlugin, filePath: String) {
        val config = getOrExtractFile(plugin, filePath) ?: return
        SoundRegistry.append(config)
    }

    /**
     * Loads a custom Texture config from an external plugin and appends it to the TextureRegistry.
     */
    fun loadCustomTextures(plugin: JavaPlugin, filePath: String) {
        val config = getOrExtractFile(plugin, filePath) ?: return
        TextureRegistry.append(config)
    }

    /**
     * Loads a custom Language config from an external plugin and appends it to the LangRegistry.
     */
    fun loadCustomLang(plugin: JavaPlugin, filePath: String) {
        val config = getOrExtractFile(plugin, filePath) ?: return
        LangRegistry.append(config)
    }

    // --- Internal File Handler ---
    private fun getOrExtractFile(plugin: JavaPlugin, filePath: String): YamlConfiguration? {
        val file = File(plugin.dataFolder, filePath)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            try {
                plugin.saveResource(filePath, false)
            } catch (e: IllegalArgumentException) {
                file.createNewFile()
            }
        }

        return try {
            YamlConfiguration.loadConfiguration(file)
        } catch (e: Exception) {
            plugin.logger.severe("[LimeFrameGUI] Failed to parse YAML file: $filePath")
            e.printStackTrace()
            null
        }
    }
}