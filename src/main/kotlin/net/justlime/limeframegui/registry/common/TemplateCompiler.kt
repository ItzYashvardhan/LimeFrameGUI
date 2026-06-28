package net.justlime.limeframegui.registry.common

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.config.FrameKeys
import net.justlime.limeframegui.config.GuiConfigHandler
import net.justlime.limeframegui.context.AnvilGuiSetting
import net.justlime.limeframegui.context.GuiSetting
import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.context.SharedContextSetting
import net.justlime.limeframegui.enums.TextCase
import net.justlime.limeframegui.models.*
import net.justlime.limeframegui.models.parser.CompiledLayers
import net.justlime.limeframegui.models.parser.ConfigLayer
import net.justlime.limeframegui.models.parser.PatternVariants
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.DynamicListMask
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.registry.component.ItemRegistry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

object TemplateCompiler {

    /**
     * The main entry point. Orchestrates the compilation of a GUI.
     */
    fun compilePage(pageId: String, config: YamlConfiguration): GuiPageTemplate? {
        val configsToParse = resolveConfigChain(pageId, config) ?: return null

        //Settings
        val setting = compileSettings(configsToParse)


        val unifiedFallbackDictionary = buildUnifiedDictionary(configsToParse)

        val dynamicCharStr = config.getString("dynamic_list.char")
        val dynamicChar = if (!dynamicCharStr.isNullOrEmpty()) dynamicCharStr.first() else null

        val layerResult = compileLayers(configsToParse, unifiedFallbackDictionary, setting.rows, dynamicChar)
        val finalPermissionItems = layerResult.permissionItems
        val masterDynamicSlotsList = layerResult.dynamicSlots

        val dynamicMask = buildDynamicMask(config, masterDynamicSlotsList)
        validateDynamicNavigation(pageId, dynamicMask, finalPermissionItems)

        return GuiPageTemplate(pageId, setting, finalPermissionItems, dynamicMask)
    }

    /**
     * Compile Anvil UI
     */
    fun compileAnvil(anvilId: String, config: YamlConfiguration): AnvilGuiSetting? {
        val configsToParse = resolveConfigChain(anvilId, config) ?: return null
        //Settings
        val baseContext = compileSettings(configsToParse)



        val anvilSetting = GuiConfigHandler.loadAnvilSetting(config)
        anvilSetting.title = baseContext.title
        if (baseContext.label.isNotEmpty()) {
            anvilSetting.label = baseContext.label
        }

        // Apply typography rules
        anvilSetting.style.textSettings = baseContext.style.textSettings.clone()
        anvilSetting.leftItem.style.textSettings = baseContext.style.textSettings.clone()
        anvilSetting.rightItem.style.textSettings = baseContext.style.textSettings.clone()
        anvilSetting.outPutItem.style.textSettings = baseContext.style.textSettings.clone()

        // Inject Context Properties (Variables, Placeholders, Requirements)
        anvilSetting.openRequirements = baseContext.openRequirements
        anvilSetting.denyBehavior = baseContext.denyBehavior
        anvilSetting.localVariables = baseContext.localVariables
        anvilSetting.localPlaceholders = baseContext.localPlaceholders

        return anvilSetting
    }


    /**
     * Parsing helper to prevent repetitive code for title, name, and lore rules.
     * Modifies properties in-place so parents bleed through cleanly until overridden!
     */
    fun parseTextGroupRule(textSection: ConfigurationSection, subPath: String, targetRule: TextFormatRule) {
        val sec = textSection.getConfigurationSection(subPath) ?: return

        if (sec.contains(FrameKeys.Text.FONT)) {
            targetRule.font = sec.getBoolean(FrameKeys.Text.FONT)
        }

        if (sec.contains(FrameKeys.Text.PREFIX)) {
            targetRule.prefix = sec.getString(FrameKeys.Text.PREFIX)
        }

        if (sec.contains(FrameKeys.Text.SUFFIX)) {
            targetRule.suffix = sec.getString(FrameKeys.Text.SUFFIX)
        }

        if (sec.contains(FrameKeys.Text.WRAP_LENGTH)) {
            targetRule.wrapLength = sec.getInt(FrameKeys.Text.WRAP_LENGTH)
        }

        if (sec.contains(FrameKeys.Text.WEIGHT)) {
            targetRule.weights = if (sec.isList(FrameKeys.Text.WEIGHT)) {
                sec.getStringList(FrameKeys.Text.WEIGHT)
            } else {
                sec.getString(FrameKeys.Text.WEIGHT)?.let { listOf(it) }
            }
        }

        if (sec.contains(FrameKeys.Text.CASE)) {
            val caseStr = sec.getString(FrameKeys.Text.CASE)?.replace("-", "_")?.uppercase()
            if (caseStr != null) {
                runCatching { targetRule.textCase = TextCase.valueOf(caseStr) }
            }
        }
    }

    /**
     * Resolves the inheritance chain and returns the list of configurations to parse.
     * Returns null if the config is a template/interface (and registers it automatically).
     */
    private fun resolveConfigChain(id: String, config: YamlConfiguration): List<ConfigLayer>? {
        val mainSection = config.getConfigurationSection(FrameKeys.Main.SECTION)
        val type = mainSection?.getString("type")?.lowercase()

        if (type == FrameKeys.Main.TEMPLATE) {
            TemplateRegistry.register(id, config)
            return null //Here null means inherit
        }

        val inheritList = mainSection?.getStringList(FrameKeys.Main.INHERIT) ?: emptyList()
        val configsToParse = resolveInheritance(inheritList).toMutableList()
        val layer = ConfigLayer(config, emptyList())
        configsToParse.add(layer)

        //border <- background <-- dashboard here 0 index is border and last index always be our main page
        return configsToParse
    }

    /**
     * Iterates from parent to child to build settings. 
     * Children inherit parent properties unless explicitly overridden.
     */
    private fun compileSettings(configsToParse: List<ConfigLayer>): IContextSetting {
        val context = SharedContextSetting()
        val configurationList = configsToParse.map { it.config }

        for (cfg in configurationList) {
            val mainSec = cfg.getConfigurationSection(FrameKeys.Main.SECTION) ?: continue

            // Shared Core Metadata
            if (mainSec.contains(FrameKeys.Main.TITLE)) {
                context.title = mainSec.getString(FrameKeys.Main.TITLE) ?: context.title
            }
            if (mainSec.contains(FrameKeys.Anvil.LABEL)) {
                context.label = mainSec.getString(FrameKeys.Anvil.LABEL) ?: context.label
            }

            // Shared Session Boundaries
            if (mainSec.contains(FrameKeys.Context.OPEN_REQUIREMENTS)) {
                context.openRequirements = mainSec.getStringList(FrameKeys.Context.OPEN_REQUIREMENTS)
            }
            if (mainSec.contains(FrameKeys.Context.DENY_ACTIONS)) {
                context.denyBehavior = ActionRegistry.parseBehavior(mainSec, FrameKeys.Context.DENY_ACTIONS)
            }

            if (mainSec.contains(FrameKeys.Context.LOCAL_VARIABLES)) {
                context.localVariables += parseStringMap(mainSec, FrameKeys.Context.LOCAL_VARIABLES)
            }
            if (mainSec.contains(FrameKeys.Context.LOCAL_PLACEHOLDERS)) {
                context.localPlaceholders += parseStringMap(mainSec, FrameKeys.Context.LOCAL_PLACEHOLDERS)
            }

            // Shared Typography Rules
            val textSec = mainSec.getConfigurationSection(FrameKeys.Text.SECTION)
            if (textSec != null) {
                parseTextGroupRule(textSec, FrameKeys.Main.TITLE, context.style.textSettings.title)
                parseTextGroupRule(textSec, FrameKeys.Item.NAME, context.style.textSettings.name)
                parseTextGroupRule(textSec, FrameKeys.Item.LORE, context.style.textSettings.lore)
            }
        }

        return context
    }

    private fun compileGuiSetting(configsToParse: List<ConfigLayer>, baseContext: IContextSetting): GuiSetting {
        var rows = FrameKeys.Default.DEFAULT_ROWS

        // Find rows specifically from parent down to child
        for (layer in configsToParse) {
            val mainSec = layer.config.getConfigurationSection(FrameKeys.Main.SECTION) ?: continue
            if (mainSec.contains(FrameKeys.Main.ROWS)) {
                rows = mainSec.getInt(FrameKeys.Main.ROWS, rows)
            }
        }

        return GuiSetting(
            rows = rows,
            title = baseContext.title,
            style = baseContext.style.copy(),
            openRequirements = baseContext.openRequirements,
            denyBehavior = baseContext.denyBehavior,
            localVariables = baseContext.localVariables,
            localPlaceholders = baseContext.localPlaceholders
        )
    }

    private fun compileAnvilSetting(configsToParse: List<ConfigLayer>, baseContext: IContextSetting): AnvilGuiSetting {
        // 1. Load basic layout using your multi-layer config pipeline
        val anvilSetting = GuiConfigHandler.loadAnvilSetting(configsToParse)

        // 2. Map basic structural metadata
        anvilSetting.title = baseContext.title
        if (baseContext is SharedContextSetting) {
            anvilSetting.label = baseContext.label
        }

        // 3. Deep-copy global typography rules to individual slot instances
        anvilSetting.style.textSettings = baseContext.style.textSettings.clone()
        anvilSetting.leftItem.style.textSettings = baseContext.style.textSettings.clone()
        anvilSetting.rightItem.style.textSettings = baseContext.style.textSettings.clone()
        anvilSetting.outPutItem.style.textSettings = baseContext.style.textSettings.clone()

        // 4. Bind context variables
        anvilSetting.openRequirements = baseContext.openRequirements
        anvilSetting.denyBehavior = baseContext.denyBehavior
        anvilSetting.localVariables = baseContext.localVariables
        anvilSetting.localPlaceholders = baseContext.localPlaceholders

        return anvilSetting
    }

    /**
     * Safely extracts a ConfigurationSection into a Map of Strings.
     */
    private fun parseStringMap(section: ConfigurationSection, key: String): Map<String, String> {
        val targetSec = section.getConfigurationSection(key) ?: return emptyMap()
        val map = mutableMapOf<String, String>()

        targetSec.getKeys(false).forEach { k ->
            map[k] = targetSec.getString(k) ?: ""
        }

        return map
    }

    /**
     * Pools all character definitions from all files into a single dictionary.
     */
    private fun buildUnifiedDictionary(configsToParse: List<ConfigLayer>): Map<Char, List<GuiItem>> {
        val unifiedMap = mutableMapOf<Char, MutableList<GuiItem>>()

        for ((cfg, _) in configsToParse) {
            val ingredientsSection = cfg.getConfigurationSection(FrameKeys.Main.INGREDIENTS)
            if (ingredientsSection != null) {
                for (key in ingredientsSection.getKeys(false)) {
                    val itemSec = ingredientsSection.getConfigurationSection(key) ?: continue
                    val item = GuiConfigHandler.loadItem(itemSec)
                    unifiedMap.getOrPut(key.first()) { mutableListOf() }.add(item)
                }
            }
            for (key in cfg.getKeys(false)) {
                if (key == FrameKeys.Main.SECTION || key == FrameKeys.Main.INGREDIENTS || key == FrameKeys.Main.DYNAMIC_LIST) continue
                val itemSec = cfg.getConfigurationSection(key) ?: continue
                val charStr = cfg.getString("$key.${FrameKeys.Item.CHAR}")
                if (!charStr.isNullOrEmpty()) {
                    val item = GuiConfigHandler.loadItem(itemSec)

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
        configsToParse: List<ConfigLayer>,
        unifiedFallbackDictionary: Map<Char, List<GuiItem>>,
        totalRows: Int,
        dynamicChar: Char?
    ): CompiledLayers {

        val permissionLayouts = mutableMapOf<String, MutableMap<Int, MutableList<GuiItem>>>()
        val explicitSlotLayouts = mutableMapOf<String, MutableMap<Int, MutableList<GuiItem>>>()
        val masterDynamicSlots = mutableSetOf<Int>()
        permissionLayouts[FrameKeys.Default.DEFAULT] = mutableMapOf()

        for ((cfg, excludedChars) in configsToParse) {
            val rawVariants = extractPatternVariants(cfg, totalRows)
            val localDictionary = mutableMapOf<Char, MutableList<GuiItem>>()
            val localExplicitItems = mutableListOf<GuiItem>()

            val ingredientsSection = cfg.getConfigurationSection(FrameKeys.Main.INGREDIENTS)
            if (ingredientsSection != null) {
                for (charKey in ingredientsSection.getKeys(false)) {
                    val char = charKey.first()
                    if (excludedChars.contains(char)) continue
                    val itemSec = ingredientsSection.getConfigurationSection(charKey) ?: continue
                    localDictionary.getOrPut(char) { mutableListOf() }.add(GuiConfigHandler.loadItem(itemSec))
                }
            }

            for (key in cfg.getKeys(false)) {
                if (key == FrameKeys.Main.SECTION || key == FrameKeys.Main.INGREDIENTS || key == FrameKeys.Main.DYNAMIC_LIST) continue
                val itemSec = cfg.getConfigurationSection(key) ?: continue
                val guiItem = GuiConfigHandler.loadItem(itemSec)
                val charStr = cfg.getString("$key.${FrameKeys.Item.CHAR}")

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

            val targetPermissions = if (rawVariants.isDefaultOnly) {
                val existingPerms = permissionLayouts.keys.toList()
                existingPerms.ifEmpty { listOf(FrameKeys.Default.DEFAULT) }
            } else rawVariants.permissions.toList()


            for (perm in targetPermissions) {
                val patternMap = rawVariants.getLayout(perm) ?: continue
                val masterLayout = permissionLayouts.computeIfAbsent(perm) { mutableMapOf() }
                val explicitLayout = explicitSlotLayouts.computeIfAbsent(perm) { mutableMapOf() }

                val sortedRows = patternMap.entries.sortedBy { it.key }.map { it.value }
                var currentSlot = 0

                for (row in sortedRows) {
                    val cleanRow = row.replace(" ", "")
                    for (char in cleanRow) {
                        when (char) {
                            dynamicChar -> {
                                masterDynamicSlots.add(currentSlot)
                                masterLayout.remove(currentSlot)
                            }

                            '!' -> masterLayout[currentSlot] = mutableListOf(GuiItem().apply { slot = currentSlot })
                            '.', '_' -> {}
                            else -> {
                                val itemsToPlace = if (excludedChars.contains(char)) {
                                    unifiedFallbackDictionary[char] ?: ItemRegistry.getByChar(char)?.let { listOf(it) }
                                } else {
                                    localDictionary[char] ?: unifiedFallbackDictionary[char] ?: ItemRegistry.getByChar(
                                        char
                                    )?.let { listOf(it) }
                                }

                                if (itemsToPlace != null) {
                                    val mappedItems = itemsToPlace.map { it.clone().apply { slot = currentSlot } }
                                    // FIX 1: Add at index 0! This ensures child layers sit on top of parent layers during Priority 0 ties.
                                    masterLayout.getOrPut(currentSlot) { mutableListOf() }.addAll(0, mappedItems)
                                }
                            }
                        }
                        currentSlot++
                    }
                }

                for (explicitItem in localExplicitItems) {
                    // FIX 2: Add explicit slot items at index 0 as well!
                    explicitItem.slot?.let {
                        explicitLayout.getOrPut(it) { mutableListOf() }.add(0, explicitItem.clone())
                    }
                    explicitItem.slotList.forEach {
                        explicitLayout.getOrPut(it) { mutableListOf() }.add(0, explicitItem.clone())
                    }
                }
            }
        }

        val finalPermissionItems = mutableMapOf<String, List<GuiItem>>()
        for ((perm, layout) in permissionLayouts) {
            val explicitOverrides = explicitSlotLayouts[perm] ?: emptyMap()
            for ((slot, items) in explicitOverrides) {
                // Add explicit overrides to the front of the line
                layout.getOrPut(slot) { mutableListOf() }.addAll(0, items)
            }
            // We no longer need to sort here because GuiSession handles the priority sorting automatically!
            finalPermissionItems[perm] = layout.values.flatten()
        }

        return CompiledLayers(finalPermissionItems, masterDynamicSlots.toList())
    }

    /**
     * Builds the dynamic mask logic for pagination limits, templates, and bounds.
     */
    private fun buildDynamicMask(config: YamlConfiguration, masterDynamicSlotsList: List<Int>): DynamicListMask? {
        val dynamicListSection = config.getConfigurationSection(FrameKeys.Main.DYNAMIC_LIST)
        if (dynamicListSection == null || masterDynamicSlotsList.isEmpty()) return null

        val populatorId = dynamicListSection.getString("populator_id") ?: ""
        val parsedTemplates = mutableMapOf<String, GuiItem>()
        if (dynamicListSection.contains("template")) {
            dynamicListSection.getConfigurationSection("template")?.let {
                parsedTemplates["default"] = GuiConfigHandler.loadItem(it)
            }
        }

        //  Parse the multi-template format
        val templatesSection = dynamicListSection.getConfigurationSection("templates")
        if (templatesSection != null) {
            for (key in templatesSection.getKeys(false)) {
                val sec = templatesSection.getConfigurationSection(key) ?: continue
                parsedTemplates[key] = GuiConfigHandler.loadItem(sec)
            }
        }

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
        visitedPath: Set<String> = emptySet()
    ): List<ConfigLayer> {
        val resolvedConfigs = mutableListOf<ConfigLayer>()

        for (inheritString in inheritList) {
            val parts = inheritString.split(" ")
            val templateId = parts[0]
            val cherryPickedChars = parts.drop(1).mapNotNull { it.removePrefix("--").firstOrNull() }

            if (visitedPath.contains(templateId)) {
                LimeFrameAPI.getPlugin().logger.warning("[LimeFrameGUI] WARNING: Circular inheritance detected at '$templateId'. Skipping.")
                continue
            }

            val templateConfig = TemplateRegistry.get(templateId) ?: continue
            val newVisitedPath = visitedPath + templateId
            // Resolve the key safely
            val inheritKey = "${FrameKeys.Main.SECTION}.${FrameKeys.Main.INHERIT}"

            // Go deeper if there are parents
            val parentInherits = templateConfig.getStringList(inheritKey)
            if (parentInherits.isNotEmpty()) {
                resolvedConfigs.addAll(resolveInheritance(parentInherits, newVisitedPath))
            }

            // Add this layer
            val layer = ConfigLayer(templateConfig, cherryPickedChars)
            resolvedConfigs.add(layer)
        }

        return resolvedConfigs
    }

    /**
     * Extracts the visual pattern variants from the configuration based on permissions.
     * Returns a map of Permission -> Map(RowIndex -> PatternString).
     */
    private fun extractPatternVariants(config: YamlConfiguration, targetRows: Int): PatternVariants {
        val layouts = mutableMapOf<String, MutableMap<Int, String>>()
        val patternPath = FrameKeys.Main.SECTION + "." + FrameKeys.Main.PATTERN

        // Child page format (Direct List) -> Maps to "default"
        if (config.isList(patternPath)) {
            val list = config.getStringList(patternPath)
            val defaultMap = mutableMapOf<Int, String>()
            list.forEachIndexed { index, row -> defaultMap[index] = row }
            layouts[FrameKeys.Default.DEFAULT] = defaultMap
            return PatternVariants(layouts)
        }

        // Template format (Mapped by Row Count / Permissions)
        val patternSection = config.getConfigurationSection(patternPath) ?: return PatternVariants(layouts)

        for (key in patternSection.getKeys(false)) {
            if (key.toIntOrNull() != null) {
                // Format: '3: [ ... ]' (No permissions, mapped to default)
                if (key.toInt() == targetRows) {
                    val list = patternSection.getStringList(key)
                    val defaultMap = mutableMapOf<Int, String>()
                    list.forEachIndexed { index, row -> defaultMap[index] = row }
                    layouts[FrameKeys.Default.DEFAULT] = defaultMap
                }
            } else {
                // Format: 'player.theme.blue: 3: [ ... ]' (Permission based)
                val list = patternSection.getStringList("$key.$targetRows")
                if (list.isNotEmpty()) {
                    val permMap = mutableMapOf<Int, String>()
                    list.forEachIndexed { index, row -> permMap[index] = row }
                    layouts[key] = permMap
                }
            }
        }

        return PatternVariants(layouts)
    }
}