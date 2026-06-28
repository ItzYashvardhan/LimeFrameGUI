package net.justlime.limeframegui.config

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.registry.common.TemplateCompiler
import net.justlime.limeframegui.registry.common.TemplateRegistry
import net.justlime.limeframegui.registry.component.*
import net.justlime.limeframegui.registry.gui.PageRegistry
import net.justlime.limeframegui.registry.input.InputRegistry
import net.justlime.limeframegui.util.ConfigErrorHandler
import net.justlime.limeframegui.util.extractDefaultsFromJar
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * File system orchestrator responsible for reading, compiling, and loading
 * all GUI configurations, components, and language files into memory.
 */
object GuiDirectoryHandler {

    lateinit var plugin: JavaPlugin

    /**
     * Initializes the directory structure, extracts default files if missing,
     * and sequentially executes the loading phases.
     *
     * @param plugin The JavaPlugin instance.
     * @param baseDir The root folder name for GUI configurations.
     */
    fun loadAll(plugin: JavaPlugin, baseDir: String = "gui") {
        this.plugin = plugin
        val guiFolder = File(plugin.dataFolder, baseDir)
        if (!guiFolder.exists()) guiFolder.mkdirs()
        extractDefaultsFromJar(plugin, baseDir)
        loadPlaceholders(File(guiFolder, "component"))
        loadLocale(guiFolder)
        loadComponents(File(guiFolder, "component"))
        loadInputs(plugin, File(guiFolder, "inputs"))
        loadPages(plugin, File(guiFolder, "pages"))
    }

    /**
     * Parses the global placeholders file to prepare static variables for load-time injection.
     */
    private fun loadPlaceholders(componentsFolder: File) {
        if (!componentsFolder.exists()) componentsFolder.mkdirs()
        val placeholderFile = File(componentsFolder, "placeholders.yml")
        if (placeholderFile.exists()) {
            try {
                PlaceholderRegistry.load(YamlFileHandler(placeholderFile).config)
            } catch (e: Exception) {
                ConfigErrorHandler.printFriendlyError(plugin.logger, placeholderFile, e)
            }
        } else PlaceholderRegistry.clear()
    }

    /**
     * Scans the language directory and compiles localized string caches.
     */
    private fun loadLocale(guiFolder: File) {
        val langFolder = File(guiFolder, "lang")
        if (langFolder.exists()) {
            val langFiles = langFolder.listFiles()?.filter { it.extension == "yml" } ?: emptyList()
            for (file in langFiles) {
                try {
                    val fileHandler = YamlFileHandler(file)
                    LangRegistry.loadLocale(fileHandler.config, file.nameWithoutExtension, true)
                } catch (e: Exception) {
                    ConfigErrorHandler.printFriendlyError(plugin.logger, file, e)
                }
            }
        }
    }

    /**
     * Processes structural UI components and global items into their respective registries.
     */
    private fun loadComponents(componentsFolder: File) {
        compileAndLoad(componentsFolder, "font") { FontRegistry.load(it) }
        compileAndLoad(componentsFolder, "sound") { SoundRegistry.load(it) }
        compileAndLoad(componentsFolder, "texture") { TextureRegistry.load(it) }
        compileAndLoad(componentsFolder, "actions") { ActionRegistry.load(it) }
        compileAndLoad(componentsFolder, "items") { ItemRegistry.load(it) }
        compileAndLoad(componentsFolder, "states") { StateRegistry.load(it) }
    }

    /**
     * Merges a root YAML file and any nested files within a corresponding directory
     * into a unified configuration map before passing it to the target registry.
     */
    private fun compileAndLoad(
        componentsFolder: File,
        componentName: String,
        registryLoadFunc: (ConfigurationSection) -> Unit
    ) {
        val masterConfig = YamlConfiguration()

        // 1. Base Files (e.g., component/actions.yml)
        val baseFile = File(componentsFolder, "$componentName.yml")
        if (baseFile.exists()) {
            try {
                val config = YamlFileHandler(baseFile).config
                for (key in config.getKeys(true)) {
                    if (config.isConfigurationSection(key)) continue
                    masterConfig.set(key, config.get(key))
                    masterConfig.set("$componentName:$key", config.get(key))
                }
            } catch (e: Exception) {
                ConfigErrorHandler.printFriendlyError(plugin.logger, baseFile, e)
            }
        }

        // 2. Subfolder Files (e.g., component/actions/dashboard.yml)
        val subFolder = File(componentsFolder, componentName)
        if (subFolder.exists() && subFolder.isDirectory) {
            subFolder.walk().filter { it.isFile && it.extension == "yml" }.forEach { file ->
                val config = YamlFileHandler(file).config

                val relativePath = file.relativeTo(subFolder).path
                    .removeSuffix(".yml")
                    .replace(File.separator, ".")

                for (key in config.getKeys(false)) {
                    val namespace = "$relativePath.$key"
                    masterConfig.set(namespace, config.get(key))
                    masterConfig.set("$componentName:$namespace", config.get(key))
                }
            }
        }

        if (masterConfig.getKeys(false).isNotEmpty()) {
            registryLoadFunc(masterConfig)
        }
    }

    private fun loadInputs(plugin: JavaPlugin, inputsFolder: File) {
        if (!inputsFolder.exists()) inputsFolder.mkdirs()

        val yamlFiles = inputsFolder.walk().filter { it.isFile && it.extension == "yml" }.toList()

        for (file in yamlFiles) {
            try {
                val fileHandler = YamlFileHandler(file)
                val relativePath = file.relativeTo(inputsFolder).path
                val inputId = relativePath.removeSuffix(".yml").replace("\\", "/")

                val anvilSetting = TemplateCompiler.compileAnvil(inputId, fileHandler.config)
                if (anvilSetting != null) {
                    InputRegistry.register(inputId, anvilSetting)
                }
            } catch (e: Exception) {
                ConfigErrorHandler.printFriendlyError(plugin.logger, file, e)
            }
        }
        plugin.logger.info("[LimeFrameGUI] Successfully indexed ${InputRegistry.getInputs().size} anvil inputs.")
    }

    /**
     * Executes the two-pass loading system for UI layouts.
     * Pass 1: Identifies and caches templates to satisfy inheritance chains.
     * Pass 2: Compiles and caches standard pages utilizing the cached templates.
     */
    private fun loadPages(plugin: JavaPlugin, pagesFolder: File) {
        if (!pagesFolder.exists()) pagesFolder.mkdirs()

        PageRegistry.clear()
        TemplateRegistry.clear()

        val yamlFiles = pagesFolder.walk().filter { it.isFile && it.extension == "yml" }.toList()
        val standardPages = mutableListOf<File>()

        for (file in yamlFiles) {
            try {
                val fileHandler = YamlFileHandler(file)
                val finalKey = FrameKeys.Main.SECTION + "." + FrameKeys.Main.TYPE

                val type = fileHandler.config.getString(finalKey)?.lowercase()

                if (type == "interface" || type == "template") {
                    val relativePath = file.relativeTo(pagesFolder).path
                    val pageId = relativePath.removeSuffix(".yml").replace("\\", "/")
                    TemplateCompiler.compilePage(pageId, fileHandler.config)
                } else {
                    standardPages.add(file)
                }
            } catch (e: Exception) {
                ConfigErrorHandler.printFriendlyError(plugin.logger, file, e)
            }
        }

        for (file in standardPages) {
            try {
                val fileHandler = YamlFileHandler(file)
                val relativePath = file.relativeTo(pagesFolder).path
                val pageId = relativePath.removeSuffix(".yml").replace("\\", "/")

                val template = TemplateCompiler.compilePage(pageId, fileHandler.config)
                if (template != null) {
                    PageRegistry.register(template)
                }
            } catch (e: Exception) {
                ConfigErrorHandler.printFriendlyError(plugin.logger, file, e)
            }
        }

        plugin.logger.info("[LimeFrameGUI] Successfully indexed ${PageRegistry.getPages().size} pages.")
    }

    /**
     * Safely flushes all memory caches and re-compiles the entire GUI directory.
     *
     * @param reset If true, overwrites existing config files with fresh JAR defaults (Dev Mode).
     */
    fun reload(plugin: JavaPlugin, reset: Boolean = false, baseDir: String = "gui") {
        plugin.logger.info("[LimeFrameGUI] Initiating reload sequence...")

        // Flush all memory registries\
        PlaceholderRegistry.clear()
        InputRegistry.clear()
        PageRegistry.clear()
        TemplateRegistry.clear()
        ItemRegistry.clear()
        SoundRegistry.clear()
        ActionRegistry.clear()
        TextureRegistry.clear()
        FontRegistry.clear()
        StateRegistry.clear()

        // Extract files
        val guiFolder = File(plugin.dataFolder, baseDir)
        if (!guiFolder.exists()) guiFolder.mkdirs()

        if (reset) {
            plugin.logger.warning("[LimeFrameGUI] DEV MODE: Overwriting config files with JAR defaults!")
        }
        extractDefaultsFromJar(plugin, baseDir, replace = reset)

        // Re-compile everything
        loadPlaceholders(File(guiFolder, "component"))
        loadLocale(guiFolder)
        loadComponents(File(guiFolder, "component"))
        loadInputs(plugin, File(guiFolder, "inputs"))
        loadPages(plugin, File(guiFolder, "pages"))

        plugin.logger.info("[LimeFrameGUI] Reload sequence complete.")
    }
}