package net.justlime.limeframegui.loader

import net.justlime.limeframegui.api.LimeFrameAPI
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * A singleton object responsible for loading and parsing the versioned small caps
 * font mappings from a `font.yml` file.
 */
object FontLoader {

    private val fontMapV_19 = mapOf(
        "a" to "ᴀ",
        "b" to "ʙ",
        "c" to "ᴄ",
        "d" to "ᴅ",
        "e" to "ᴇ",
        "f" to "ꜰ",
        "g" to "ɢ",
        "h" to "ʜ",
        "i" to "ɪ",
        "j" to "ᴊ",
        "k" to "ᴋ",
        "l" to "ʟ",
        "m" to "ᴍ",
        "n" to "ɴ",
        "o" to "ᴏ",
        "p" to "ᴘ",
        "q" to "ǫ",
        "r" to "ʀ",
        "s" to "s",
        "t" to "ᴛ",
        "u" to "ᴜ",
        "v" to "ᴠ",
        "w" to "ᴡ",
        "x" to "x",
        "y" to "ʏ",
        "z" to "ᴢ",
        "0" to "𝟬",
        "1" to "𝟭",
        "2" to "𝟮",
        "3" to "𝟯",
        "4" to "𝟰",
        "5" to "𝟱",
        "6" to "𝟲",
        "7" to "𝟳",
        "8" to "𝟴",
        "9" to "𝟵",
        "@" to "＠"
    )
    private val fontMapV_16 = mapOf(
        "a" to "ᴀ",
        "b" to "ʙ",
        "c" to "ᴄ",
        "d" to "ᴅ",
        "e" to "ᴇ",
        "f" to "ꜰ",
        "g" to "ɢ",
        "h" to "ʜ",
        "i" to "ɪ",
        "j" to "ᴊ",
        "k" to "ᴋ",
        "l" to "ʟ",
        "m" to "ᴍ",
        "n" to "ɴ",
        "o" to "ᴏ",
        "p" to "ᴘ",
        "q" to "ǫ",
        "r" to "ʀ",
        "s" to "s",
        "t" to "ᴛ",
        "u" to "ᴜ",
        "v" to "ᴠ",
        "w" to "ᴡ",
        "x" to "x",
        "y" to "ʏ",
        "z" to "ᴢ",
    )

    /**
     * Set Font for specific or newer Minecraft Version.
     */
    val defaultFontMap = mapOf("1.20" to fontMapV_19, "1.16" to fontMapV_16)

    /**
     * The loaded font map.
     * The outer key is the Minecraft version string (e.g., "1.19").
     * The inner map contains the character-to-font mappings.
     */
    var capsFont: Map<String, Map<String, String>> = defaultFontMap
        private set

    /**
     * Loads the font-type yml file from the plugin's data folder.
     * If the file does not exist, it will be created from the plugin's resources.
     * It then parses the file into the `smallCapsFont` map.
     */
    fun load(fileName: String) {
        val plugin = LimeFrameAPI.getPlugin()
        try {
            val fontFile = File(plugin.dataFolder, fileName)
            if (!fontFile.exists()) {
                plugin.saveResource(fileName, false)
            }
            if (fontFile.exists()) {
                val config = YamlConfiguration.loadConfiguration(fontFile)
                if (config.getKeys(false).isEmpty()) {
                    return
                }

                val loadedMap = mutableMapOf<String, Map<String, String>>()

                // Loop through keys (e.g., "19", "20", "26.1")
                for (versionKey in config.getKeys(false)) {
                    val versionSection = config.getConfigurationSection(versionKey) ?: continue
                    val characterMap = mutableMapOf<String, String>()

                    for (charKey in versionSection.getKeys(false)) {
                        versionSection.getString(charKey)?.let { characterMap[charKey] = it }
                    }

                    // LOGIC CHANGE:
                    // If key has a dot (e.g. "26.1" or "1.20.4"), use it as is.
                    // If key is just a number (e.g. "19"), treat it as legacy "1.19".
                    val finalVersionKey = if (versionKey.contains(".")) {
                        versionKey
                    } else {
                        "1.$versionKey"
                    }

                    loadedMap[finalVersionKey] = characterMap
                }
                capsFont = loadedMap

            } else {
                loadDefaultFonts()
            }
        } catch (e: Exception) {
            loadDefaultFonts()
        }
    }

    private fun loadDefaultFonts() {
        capsFont = defaultFontMap
    }
}