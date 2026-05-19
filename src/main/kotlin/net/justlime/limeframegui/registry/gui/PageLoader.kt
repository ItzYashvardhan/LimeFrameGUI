package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.config.GuiConfigHandler
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiPageTemplate
import net.justlime.limeframegui.models.registry.DynamicListMask
import net.justlime.limeframegui.registry.component.ItemRegistry
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration

/**
 * The core engine responsible for parsing YAML configurations into memory-ready GUI templates.
 * Handles complex UI features including deep template inheritance, pattern overlaying,
 * character cherry-picking, and dynamic list mapping.
 */
object PageLoader {

    /**
     * Parses a given YAML configuration into a [GuiPageTemplate].
     * If the configuration is defined as an interface/template, it registers it to the [TemplateRegistry]
     * and returns null. Otherwise, it compiles the inheritance tree, patterns, and items.
     */
    fun parseTemplate(pageId: String, config: YamlConfiguration): GuiPageTemplate? {
        val mainSection = config.getConfigurationSection(LimeFrameAPI.keys.main)
        val type = mainSection?.getString("type")?.lowercase()

        // 1. Template Validation (Warning Removed!)
        if (type == "interface" || type == "template") {
            TemplateRegistry.register(pageId, config)
            return null
        }

        val setting = GuiConfigHandler.loadInventorySetting(mainSection)
        val dynamicCharStr = config.getString("dynamic_list.char")
        val dynamicChar = if (!dynamicCharStr.isNullOrEmpty()) dynamicCharStr.first() else null

        // 2. Compile Inheritance Chain
        val inheritList = mainSection?.getStringList("inherit") ?: emptyList()
        val configsToParse = resolveInheritance(inheritList).toMutableList()
        configsToParse.add(Pair(config, emptyList()))

        // 🌟 Build the Unified Fallback Dictionary
        // This solves the "Interface" problem. If a parent places a pattern but
        // a child defines the item, this dictionary bridges the gap!
        val unifiedFallbackDictionary = mutableMapOf<Char, GuiItem>()
        for ((cfg, _) in configsToParse) {
            val ingredientsSection = cfg.getConfigurationSection("ingredients")
            if (ingredientsSection != null) {
                for (key in ingredientsSection.getKeys(false)) {
                    val itemSec = ingredientsSection.getConfigurationSection(key) ?: continue
                    unifiedFallbackDictionary[key.first()] = GuiConfigHandler.loadItem(itemSec)
                }
            }
            for (key in cfg.getKeys(false)) {
                if (key == "main" || key == "ingredients" || key == "dynamic_list") continue
                val itemSec = cfg.getConfigurationSection(key) ?: continue
                val charStr = cfg.getString("$key.char")
                if (!charStr.isNullOrEmpty()) {
                    unifiedFallbackDictionary[charStr.first()] = GuiConfigHandler.loadItem(itemSec)
                }
            }
        }

        // 3. The "Photoshop Layers" Rendering Engine
        val permissionLayouts = mutableMapOf<String, MutableMap<Int, GuiItem>>()
        val explicitSlotLayouts = mutableMapOf<String, MutableMap<Int, GuiItem>>()
        val masterDynamicSlots = mutableSetOf<Int>()

        permissionLayouts["default"] = mutableMapOf()

        // Process from oldest Parent to newest Child
        for ((cfg, excludedChars) in configsToParse) {
            val rawVariants = extractPatternVariants(cfg, setting.rows)

            // A. Build this specific file's Local Dictionary
            val localDictionary = mutableMapOf<Char, GuiItem>()
            val localExplicitItems = mutableListOf<GuiItem>()

            val ingredientsSection = cfg.getConfigurationSection("ingredients")
            if (ingredientsSection != null) {
                for (charKey in ingredientsSection.getKeys(false)) {
                    val char = charKey.first()
                    if (excludedChars.contains(char)) continue
                    val itemSec = ingredientsSection.getConfigurationSection(charKey) ?: continue
                    localDictionary[char] = GuiConfigHandler.loadItem(itemSec)
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
                        localDictionary[char] = guiItem
                    }
                }

                if (guiItem.slot != null || guiItem.slotList.isNotEmpty()) {
                    localExplicitItems.add(guiItem)
                }
            }

            // B. Paint this file's pattern onto the Master Layout
            val targetPermissions = if (rawVariants.size == 1 && rawVariants.containsKey("default")) {
                val existingPerms = permissionLayouts.keys.toList()
                if (existingPerms.isEmpty()) listOf("default") else existingPerms
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
                            '!' -> masterLayout[currentSlot] = GuiItem(Material.AIR).apply { slot = currentSlot }
                            '.', '_' -> { /* Transparency: Leave the master layout untouched! */ }
                            else -> {
                                if (excludedChars.contains(char)) {
                                    // 🌟 GLOBAL OVERRIDE TRIGGERED (--W)
                                    // Skip the parent's definition and force it to use the child's!
                                    val overrideItem = unifiedFallbackDictionary[char] ?: ItemRegistry.getByChar(char)
                                    if (overrideItem != null) {
                                        masterLayout[currentSlot] = overrideItem.clone().apply { this.slot = currentSlot }
                                    }
                                } else {
                                    // 🌟 NORMAL LAYERS (No --W)
                                    // 1. Check local dictionary (e.g. Parent's W = White Glass)
                                    // 2. Fallback to unified dictionary (e.g. Parent placed Y, but Child defined Y)
                                    // 3. Fallback to global server registry
                                    val item = localDictionary[char] ?: unifiedFallbackDictionary[char] ?: ItemRegistry.getByChar(char)
                                    if (item != null) {
                                        masterLayout[currentSlot] = item.clone().apply { this.slot = currentSlot }
                                    }
                                }
                            }
                        }
                        currentSlot++
                    }
                }

                // C. Queue explicit slots (e.g. `slot: 13`) to override the pattern
                for (explicitItem in localExplicitItems) {
                    explicitItem.slot?.let { explicitLayout[it] = explicitItem.clone() }
                    explicitItem.slotList.forEach { explicitLayout[it] = explicitItem.clone() }
                }
            }
        }

        // 4. Finalize Compilation
        val finalPermissionItems = mutableMapOf<String, List<GuiItem>>()
        for ((perm, layout) in permissionLayouts) {
            val explicitOverrides = explicitSlotLayouts[perm] ?: emptyMap()
            for ((slot, item) in explicitOverrides) {
                layout[slot] = item
            }
            finalPermissionItems[perm] = layout.values.toList()
        }

        // 5. Build Dynamic Mask
        var dynamicMask: DynamicListMask? = null
        val dynamicListSection = config.getConfigurationSection("dynamic_list")
        val masterDynamicSlotsList = masterDynamicSlots.toList()

        if (dynamicListSection != null && masterDynamicSlotsList.isNotEmpty()) {
            val populatorId = dynamicListSection.getString("populator_id") ?: ""
            val templateSection = dynamicListSection.getConfigurationSection("template")
            val templateItem = templateSection?.let { GuiConfigHandler.loadItem(it) }

            val bufferConfig = GuiBuffer()
            val bufferSection = dynamicListSection.getConfigurationSection("buffer")
            if (bufferSection != null) {
                bufferConfig.renderLimit = bufferSection.getInt("render-limit", bufferConfig.renderLimit)
                bufferConfig.margin = bufferSection.getInt("margin", bufferConfig.margin)
                bufferConfig.cleanupMargin = bufferSection.getInt("cleanup-margin", bufferConfig.cleanupMargin)
            }

            if (templateItem != null) {
                dynamicMask = DynamicListMask(populatorId, masterDynamicSlotsList, templateItem, bufferConfig)
            }
        }

        if (dynamicMask != null) {
            val defaultItems = finalPermissionItems["default"] ?: finalPermissionItems.values.firstOrNull() ?: emptyList()
            val hasNext = defaultItems.any { it.style.action == "core_next_page" }
            val hasPrev = defaultItems.any { it.style.action == "core_prev_page" }
            if (!hasNext || !hasPrev) {
                LimeFrameAPI.getPlugin().logger.warning("[LimeFrameGUI] SEVERE WARNING: Page '$pageId' has a dynamic list but is missing navigation actions!")
            }
        }

        return GuiPageTemplate(pageId, setting, finalPermissionItems, dynamicMask)
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
    private fun extractPatternVariants(config: YamlConfiguration, targetRows: Int): Map<String, MutableMap<Int, String>> {
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

    /**
     * Overlays a child row on top of a parent row.
     * The `.` character acts as transparency, allowing the parent's character to pass through.
     */
    private fun overlayRow(parentRow: String, childRow: String): String {
        val parentClean = parentRow.replace(" ", "")
        val childClean = childRow.replace(" ", "")

        val sb = java.lang.StringBuilder()
        val maxLength = maxOf(parentClean.length, childClean.length)

        for (i in 0 until maxLength) {
            val pChar = parentClean.getOrNull(i) ?: ' '
            val cChar = childClean.getOrNull(i) ?: ' '
            sb.append(if (cChar == '.') pChar else cChar)
        }
        return sb.toString()
    }
}