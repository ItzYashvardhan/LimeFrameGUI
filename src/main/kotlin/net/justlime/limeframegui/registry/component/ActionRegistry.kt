package net.justlime.limeframegui.registry.component

import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.GuiActionPack
import org.bukkit.configuration.ConfigurationSection

object ActionRegistry : IRegistry {
    private val actionPacks = mutableMapOf<String, GuiActionPack>()

    fun get(key: String): GuiActionPack? = actionPacks[key]

    override fun load(section: ConfigurationSection) {
        actionPacks.clear()
        flattenAndParse(section, "")
        println(actionPacks)
    }

    override fun append(section: ConfigurationSection) {
        flattenAndParse(section, "")
    }

    override fun clear() {
        actionPacks.clear()
    }

    private fun flattenAndParse(section: ConfigurationSection, pathPrefix: String) {
        for (key in section.getKeys(false)) {
            val currentPath = if (pathPrefix.isEmpty()) key else "$pathPrefix.$key"

            // Support single strings alongside lists!
            if (section.isList(key) || section.isString(key)) {
                val rawList = if (section.isList(key)) section.getStringList(key) else listOf(section.getString(key)!!)
                val resolvedList = PlaceholderRegistry.resolve(rawList)

                actionPacks[currentPath] = GuiActionPack.Standard(
                    requirements = emptyList(),
                    denyBehavior = ActionBehavior.Simple(emptyList()),
                    clickActions = mutableMapOf("click" to ActionBehavior.Simple(resolvedList))
                )
                continue
            }

            val subSection = section.getConfigurationSection(key) ?: continue

            // 1. Is it a Sequence Pack?
            if (subSection.contains("sequence")) {
                val seqSection = subSection.getConfigurationSection("sequence")!!
                val nodes = mutableListOf<GuiActionPack.Standard>()
                var fallback: GuiActionPack.Standard? = null

                val sortedKeys = seqSection.getKeys(false).sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }

                for (seqKey in sortedKeys) {
                    val nodeSection = seqSection.getConfigurationSection(seqKey) ?: continue
                    val parsedNode = parseStandardNode(nodeSection)

                    if (seqKey.equals("else", ignoreCase = true)) {
                        fallback = parsedNode
                    } else {
                        nodes.add(parsedNode)
                    }
                }
                actionPacks[currentPath] = GuiActionPack.Sequence(nodes, fallback)
            }
            // 2. Is it a Standard Pack?
            else if (isActionPack(subSection)) {
                actionPacks[currentPath] = parseStandardNode(subSection)
            }
            // 3. Just a nested folder, go deeper
            else {
                flattenAndParse(subSection, currentPath)
            }
        }
    }

    private fun parseStandardNode(section: ConfigurationSection): GuiActionPack.Standard {
        // Resolve placeholders inside requirements!
        val rawRequirements = section.getStringList("requirements")
        val requirements = PlaceholderRegistry.resolve(rawRequirements)

        val denyBehavior = parseBehavior(section, "deny")
        val clickActions = mutableMapOf<String, ActionBehavior>()

        for (subKey in section.getKeys(false)) {
            val lowerKey = subKey.lowercase()
            if (lowerKey != "requirements" && lowerKey != "deny") {
                clickActions[lowerKey] = parseBehavior(section, subKey)
            }
        }
        return GuiActionPack.Standard(requirements, denyBehavior, clickActions)
    }

    /**
     * THE MAGIC BEHAVIOR PARSER (Detects Simple Lists vs 'When' Blocks)
     */
    fun parseBehavior(section: ConfigurationSection, key: String): ActionBehavior {
        if (section.isList(key)) {
            return ActionBehavior.Simple(PlaceholderRegistry.resolve(section.getStringList(key)))
        }

        // 🌟 Catch single-string actions so they don't get ignored!
        if (section.isString(key)) {
            val singleString = section.getString(key)!!
            return ActionBehavior.Simple(listOf(PlaceholderRegistry.resolve(singleString)))
        }

        if (section.isConfigurationSection(key)) {
            val sub = section.getConfigurationSection(key)!!
            if (sub.contains("when")) {
                val whenSec = sub.getConfigurationSection("when")!!

                // 🌟 FIX 4: Resolve placeholders inside the "when" value!
                val rawValue = whenSec.getString("value") ?: ""
                val valueStr = PlaceholderRegistry.resolve(rawValue)

                val resultSec = whenSec.getConfigurationSection("result")
                val resultMap = mutableMapOf<String, ActionBehavior>()

                if (resultSec != null) {
                    for (resKey in resultSec.getKeys(false)) {
                        // RECURSION! Allows infinite nesting if needed.
                        resultMap[resKey] = parseBehavior(resultSec, resKey)
                    }
                }
                return ActionBehavior.When(valueStr, resultMap)
            }
        }
        return ActionBehavior.Simple(emptyList())
    }

    private fun isActionPack(section: ConfigurationSection): Boolean {
        return section.getKeys(false).any {
            it.lowercase().contains("click") || it == "requirements" || it == "deny"
        }
    }
}