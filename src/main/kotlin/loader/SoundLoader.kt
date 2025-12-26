package loader

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.models.GuiSound
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object SoundLoader {
    private val sounds = mutableMapOf<String, GuiSound>()

    /**
     * Loads sounds from a YAML configuration section (e.g., sounds.yml).
     * Structure:
     * my-sound: "BLOCK_NOTE_BLOCK_PLING, 1.0"
     * complex-sound:
     * - "SOUND_A, 1.0"
     * - "SOUND_B, 1.5, 1.0, 10"
     */
    fun loadConfig(section: YamlConfiguration) {
        sounds.clear()
        for (key in section.getKeys(true)) {
            val rawValue = section.get(key)
            val soundObj = GuiSound.loadSound(rawValue, ignoreRegistry = true)
            sounds[key] = soundObj
        }
    }

    fun load(fileName: String = "sounds.yml") {
        val plugin = LimeFrameAPI.getPlugin()
        val file = File(plugin.dataFolder, fileName)

        if (!file.exists()) {
            plugin.saveResource(fileName, false)
        }
        if (file.exists()) {
            val soundConfig = YamlConfiguration.loadConfiguration(file)
            loadConfig(soundConfig)
        }
    }

    /**
     * Retrieves a registered sound.
     * IMPORTANT: Returns a CLONE so modifying the result doesn't affect the registry.
     */
    fun get(id: String): GuiSound? {
        return sounds[id]?.clone()
    }

    fun getAllIds(): Set<String> = sounds.keys
}