package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.config.FrameConfigKeys
import net.justlime.limeframegui.config.GuiConfigHandler
import net.justlime.limeframegui.enums.TextCase
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiPageTemplate
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.models.registry.DynamicListMask
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.registry.component.ItemRegistry
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

object TemplateCompiler {

    /**
     * The main entry point. Orchestrates the compilation of a GUI.
     */
    fun compile(pageId: String, config: YamlConfiguration): GuiPageTemplate? {
        val mainSection = config.getConfigurationSection(LimeFrameAPI.keys.main)
        val type = mainSection?.getString("type")?.lowercase()

        if (type == "interface" || type == "template") {
            TemplateRegistry.register(pageId, config)
            return null
        }

        val inheritList = mainSection?.getStringList("inherit") ?: emptyList()
        val configsToParse = resolveInheritance(inheritList).toMutableList()
        configsToParse.add(Pair(config, emptyList()))

        val setting = compileSettings(configsToParse)

        val unifiedFallbackDictionary = buildUnifiedDictionary(configsToParse)

        val dynamicCharStr = config.getString("dynamic_list.char")
        val dynamicChar = if (!dynamicCharStr.isNullOrEmpty()) dynamicCharStr.first() else null

        val layerResult = compileLayers(configsToParse, unifiedFallbackDictionary, setting.rows, dynamicChar)
        val finalPermissionItems = layerResult.first
        val masterDynamicSlotsList = layerResult.second

        val dynamicMask = buildDynamicMask(config, masterDynamicSlotsList)
        validateDynamicNavigation(pageId, dynamicMask, finalPermissionItems)

        return GuiPageTemplate(pageId, setting, finalPermissionItems, dynamicMask)
    }

    /**
     * Iterates from parent to child to build settings. 
     * Children inherit parent properties unless explicitly overridden.
     */
    private fun compileSettings(configsToParse: List<Pair<YamlConfiguration, List<Char>>>): GuiSetting {
        val keys = LimeFrameAPI.keys
        val finalSetting = GuiSetting(rows = keys.defaultInventoryRows, title = keys.defaultInventoryTitle)

        for ((cfg, _) in configsToParse) {
            val mainSec = cfg.getConfigurationSection(keys.main) ?: continue

            // Core Dimensions & Metadata
            if (mainSec.contains(keys.inventoryRows)) {
                finalSetting.rows = mainSec.getInt(keys.inventoryRows, finalSetting.rows)
            }
            if (mainSec.contains(keys.inventoryTitle)) {
                finalSetting.title = mainSec.getString(keys.inventoryTitle) ?: finalSetting.title
            }

            // Open Requirements & Deny Handlers
            if (mainSec.contains(keys.openRequirements)) {
                finalSetting.openRequirements = mainSec.getStringList(keys.openRequirements)
            }
            if (mainSec.contains(keys.denyActions)) {
                finalSetting.denyBehavior = ActionRegistry.parseBehavior(mainSec, keys.denyActions)
            }

            // Advanced Text Formatting Section (Title, Name, Lore)
            val textSec = mainSec.getConfigurationSection(keys.textSection)
            if (textSec != null) {
                parseTextGroupRule(textSec, "title", finalSetting.style.textSettings.title, keys)
                parseTextGroupRule(textSec, "name", finalSetting.style.textSettings.name, keys)
                parseTextGroupRule(textSec, "lore", finalSetting.style.textSettings.lore, keys)
            }
        }
        return finalSetting
    }

    /**
     * parsing helper to prevent repetitive code for title, name, and lore rules.
     * Modifies properties in-place so parents bleed through cleanly until overridden!
     */
    fun parseTextGroupRule(
        textSection: ConfigurationSection,
        subPath: String,
        targetRule: TextFormatRule,
        keys: FrameConfigKeys
    ) {
        val sec = textSection.getConfigurationSection(subPath) ?: return

        if (sec.contains(keys.textFont)) {
            targetRule.font = sec.getBoolean(keys.textFont)
        }
        if (sec.contains(keys.textPrefix)) {
            targetRule.prefix = sec.getString(keys.textPrefix) ?: targetRule.prefix
        }
        if (sec.contains(keys.textSuffix)) {
            targetRule.suffix = sec.getString(keys.textSuffix) ?: targetRule.suffix
        }
        if (sec.contains(keys.textWrapLength)) {
            targetRule.wrapLength = sec.getInt(keys.textWrapLength, targetRule.wrapLength)
        }
        if (sec.contains(keys.textWeight)) {
            targetRule.weights = if (sec.isList(keys.textWeight)) sec.getStringList(keys.textWeight)
            else listOf(sec.getString(keys.textWeight)!!)
        }
        if (sec.contains(keys.textCase)) {
            val caseStr = sec.getString(keys.textCase)?.replace("-", "_")?.uppercase()
            if (caseStr != null) {
                runCatching { targetRule.textCase = TextCase.valueOf(caseStr) }
            }
        }
    }

    /**
     * Pools all character definitions from all files into a single dictionary.
     */
    private fun buildUnifiedDictionary(configsToParse: List<Pair<YamlConfiguration, List<Char>>>): Map<Char, List<GuiItem>> {
        val unifiedMap = mutableMapOf<Char, MutableList<GuiItem>>()

        for ((cfg, _) in configsToParse) {
            val ingredientsSection = cfg.getConfigurationSection("ingredients")
            if (ingredientsSection != null) {
                for (key in ingredientsSection.getKeys(false)) {
                    val itemSec = ingredientsSection.getConfigurationSection(key) ?: continue
                    val item = GuiConfigHandler.loadItem(itemSec)

                    // 🌟 Add to list instead of overwriting
                    unifiedMap.getOrPut(key.first()) { mutableListOf() }.add(item)
                }
            }
            for (key in cfg.getKeys(false)) {
                if (key == "main" || key == "ingredients" || key == "dynamic_list") continue
                val itemSec = cfg.getConfigurationSection(key) ?: continue
                val charStr = cfg.getString("$key.char")

                if (!charStr.isNullOrEmpty()) {
                    val item = GuiConfigHandler.loadItem(itemSec)

                    // 🌟 Add to list instead of overwriting
                    unifiedMap.getOrPut(charStr.first()) { mutableListOf() }.add(item)
                }
            }
        }
        return unifiedMap
    }
    /**
     * The core rendering engine. Applies patterns and explicit slots layer by layer.
     * Returns the finalized layout map and the dynamic slots list.
     */
    private fun compileLayers(
        configsToParse: List<Pair<YamlConfiguration, List<Char>>>,
        unifiedFallbackDictionary: Map<Char, List<GuiItem>>,
        totalRows: Int,
        dynamicChar: Char?
    ): Pair<Map<String, List<GuiItem>>, List<Int>> {

        val permissionLayouts = mutableMapOf<String, MutableMap<Int, MutableList<GuiItem>>>()
        val explicitSlotLayouts = mutableMapOf<String, MutableMap<Int, MutableList<GuiItem>>>()
        val masterDynamicSlots = mutableSetOf<Int>()
        permissionLayouts["default"] = mutableMapOf()

        for ((cfg, excludedChars) in configsToParse) {
            val rawVariants = extractPatternVariants(cfg, totalRows)
            val localDictionary = mutableMapOf<Char, MutableList<GuiItem>>()
            val localExplicitItems = mutableListOf<GuiItem>()

            val ingredientsSection = cfg.getConfigurationSection("ingredients")
            if (ingredientsSection != null) {
                for (charKey in ingredientsSection.getKeys(false)) {
                    val char = charKey.first()
                    if (excludedChars.contains(char)) continue
                    val itemSec = ingredientsSection.getConfigurationSection(charKey) ?: continue
                    localDictionary.getOrPut(char) { mutableListOf() }.add(GuiConfigHandler.loadItem(itemSec))
                }
            }

            for (key in cfg.getKeys(false)) {
                if (key == "main" || key == "ingredients" || key == "dynamic_list") continue
                val itemSec = cfg.getConfigurationSection(key) ?: continue
                val guiItem = GuiConfigHandler.loadItem(itemSec)
                val charStr = cfg.getString("$key.char")

                if (!charStr.isNullOrEmpty()) {
                    val char = charStr.first()
                    if (!excludedChars.contains(char)) {
                        localDictionary.getOrPut(char) { mutableListOf() }.add(guiItem)
                    }
                }
                if (guiItem.slot != null || guiItem.slotList.isNotEmpty()) {
                    localExplicitItems.add(guiItem)
                }
            }

            val targetPermissions = if (rawVariants.size == 1 && rawVariants.containsKey("default")) {
                val existingPerms = permissionLayouts.keys.toList()
                existingPerms.ifEmpty { listOf("default") }
            } else {
                rawVariants.keys.toList()
            }

            for (perm in targetPermissions) {
                val patternMap = rawVariants[perm] ?: rawVariants["default"] ?: continue
                val masterLayout = permissionLayouts.computeIfAbsent(perm) { mutableMapOf() }
                val explicitLayout = explicitSlotLayouts.computeIfAbsent(perm) { mutableMapOf() }

                val sortedRows = patternMap.entries.sortedBy { it.key }.map { it.value }
                var currentSlot = 0

                for (row in sortedRows) {
                    val cleanRow = row.replace(" ", "")
                    for (char in cleanRow) {
                        when (char) {
                            dynamicChar -> masterDynamicSlots.add(currentSlot)
                            '!' -> masterLayout[currentSlot] = mutableListOf(GuiItem(Material.AIR).apply { slot = currentSlot })
                            '.', '_' -> {}
                            else -> {
                                val itemsToPlace = if (excludedChars.contains(char)) {
                                    unifiedFallbackDictionary[char] ?: ItemRegistry.getByChar(char)?.let { listOf(it) }
                                } else {
                                    localDictionary[char] ?: unifiedFallbackDictionary[char] ?: ItemRegistry.getByChar(char)?.let { listOf(it) }
                                }

                                if (itemsToPlace != null) {
                                    masterLayout[currentSlot] = itemsToPlace.map { it.clone().apply { slot = currentSlot } }.toMutableList()
                                }
                            }
                        }
                        currentSlot++
                    }
                }

                for (explicitItem in localExplicitItems) {
                    explicitItem.slot?.let { explicitLayout.getOrPut(it) { mutableListOf() }.add(explicitItem.clone()) }
                    explicitItem.slotList.forEach { explicitLayout.getOrPut(it) { mutableListOf() }.add(explicitItem.clone()) }
                }
            }
        }

        val finalPermissionItems = mutableMapOf<String, List<GuiItem>>()
        for ((perm, layout) in permissionLayouts) {
            val explicitOverrides = explicitSlotLayouts[perm] ?: emptyMap()
            for ((slot, items) in explicitOverrides) {
                layout[slot] = items
            }
            finalPermissionItems[perm] = layout.values.flatten()
        }

        return Pair(finalPermissionItems, masterDynamicSlots.toList())
    }
    /**
     * Builds the dynamic mask logic for pagination limits, templates, and bounds.
     */
    private fun buildDynamicMask(config: YamlConfiguration, masterDynamicSlotsList: List<Int>): DynamicListMask? {
        val dynamicListSection = config.getConfigurationSection("dynamic_list")
        if (dynamicListSection == null || masterDynamicSlotsList.isEmpty()) return null

        val populatorId = dynamicListSection.getString("populator_id") ?: ""

        // 🌟 NEW: Parse multiple templates into a Map
        val parsedTemplates = mutableMapOf<String, GuiItem>()

        // Backwards compatibility: If they still use the old single 'template' key
        if (dynamicListSection.contains("template")) {
            dynamicListSection.getConfigurationSection("template")?.let {
                parsedTemplates["default"] = GuiConfigHandler.loadItem(it)
            }
        }

        // 🌟 NEW: Parse the multi-template format
        val templatesSection = dynamicListSection.getConfigurationSection("templates")
        if (templatesSection != null) {
            for (key in templatesSection.getKeys(false)) {
                val sec = templatesSection.getConfigurationSection(key) ?: continue
                parsedTemplates[key] = GuiConfigHandler.loadItem(sec)
            }
        }

        // If no templates were found at all, we can't render the list
        if (parsedTemplates.isEmpty()) return null

        val bufferConfig = GuiBuffer()
        val bufferSection = dynamicListSection.getConfigurationSection("buffer")
        if (bufferSection != null) {
            bufferConfig.renderLimit = bufferSection.getInt("render-limit", bufferConfig.renderLimit)
            bufferConfig.margin = bufferSection.getInt("margin", bufferConfig.margin)
            bufferConfig.cleanupMargin = bufferSection.getInt("cleanup-margin", bufferConfig.cleanupMargin)
        }

        return DynamicListMask(populatorId, masterDynamicSlotsList, parsedTemplates, bufferConfig)
    }

    /**
     * Triggers warnings if dynamic lists are missing crucial navigation buttons.
     */
    private fun validateDynamicNavigation(
        pageId: String,
        dynamicMask: DynamicListMask?,
        finalPermissionItems: Map<String, List<GuiItem>>
    ) {
        if (dynamicMask != null) {
            val defaultItems =
                finalPermissionItems["default"] ?: finalPermissionItems.values.firstOrNull() ?: emptyList()
            val hasNext = defaultItems.any { it.style.action == "core_next_page" }
            val hasPrev = defaultItems.any { it.style.action == "core_prev_page" }
            if (!hasNext || !hasPrev) {
                LimeFrameAPI.getPlugin().logger.warning("[LimeFrameGUI] SEVERE WARNING: Page '$pageId' has a dynamic list but is missing navigation actions!")
            }
        }
    }

    /**
     * Resolves the entire inheritance chain securely, handling nested templates and circular dependencies.
     * Parses cherry-picking arguments (e.g., "template/background --B --W") into a filter list.
     */
    private fun resolveInheritance(
        inheritList: List<String>,
        visited: MutableSet<String> = mutableSetOf()
    ): List<Pair<YamlConfiguration, List<Char>>> {
        val resolvedConfigs = mutableListOf<Pair<YamlConfiguration, List<Char>>>()

        for (inheritString in inheritList) {
            val parts = inheritString.split(" ")
            val templateId = parts[0]
            val cherryPickedChars = parts.drop(1).mapNotNull { it.removePrefix("--").firstOrNull() }

            if (visited.contains(templateId)) {
                LimeFrameAPI.getPlugin().logger.warning("[LimeFrameGUI] WARNING: Circular inheritance detected at '$templateId'. Skipping.")
                continue
            }

            val templateConfig = TemplateRegistry.get(templateId) ?: continue
            visited.add(templateId)

            val parentInherits = templateConfig.getStringList("${LimeFrameAPI.keys.main}.inherit")
            if (parentInherits.isNotEmpty()) {
                resolvedConfigs.addAll(resolveInheritance(parentInherits, visited))
            }

            resolvedConfigs.add(Pair(templateConfig, cherryPickedChars))
        }

        return resolvedConfigs
    }

    /**
     * Extracts the visual pattern variants from the configuration based on permissions.
     * Returns a map of Permission -> Map(RowIndex -> PatternString).
     */
    private fun extractPatternVariants(
        config: YamlConfiguration,
        targetRows: Int
    ): Map<String, MutableMap<Int, String>> {
        val variants = mutableMapOf<String, MutableMap<Int, String>>()
        val patternPath = LimeFrameAPI.keys.pattern

        // 1. Child page format (Direct List) -> Maps to "default"
        if (config.isList(patternPath)) {
            val list = config.getStringList(patternPath)
            val defaultMap = mutableMapOf<Int, String>()
            list.forEachIndexed { index, row -> defaultMap[index] = row }
            variants["default"] = defaultMap
            return variants
        }

        // 2. Template format (Mapped by Row Count / Permissions)
        val patternSection = config.getConfigurationSection(patternPath) ?: return variants

        for (key in patternSection.getKeys(false)) {
            if (key.toIntOrNull() != null) {
                // Format: '3: [ ... ]' (No permissions, mapped to default)
                if (key.toInt() == targetRows) {
                    val list = patternSection.getStringList(key)
                    val defaultMap = mutableMapOf<Int, String>()
                    list.forEachIndexed { index, row -> defaultMap[index] = row }
                    variants["default"] = defaultMap
                }
            } else {
                // Format: 'player.theme.blue: 3: [ ... ]' (Permission based)
                val list = patternSection.getStringList("$key.$targetRows")
                if (list.isNotEmpty()) {
                    val permMap = mutableMapOf<Int, String>()
                    list.forEachIndexed { index, row -> permMap[index] = row }
                    variants[key] = permMap
                }
            }
        }

        return variants
    }
}