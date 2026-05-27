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
    }

    override fun append(section: ConfigurationSection) {
        flattenAndParse(section, "")
    }

    override fun clear() {
        actionPacks.clear()
    }

    private fun parseSequenceNode(seqSection: ConfigurationSection): GuiActionPack.Sequence {
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
        return GuiActionPack.Sequence(nodes, fallback)
    }

    fun parseBehavior(section: ConfigurationSection, key: String): ActionBehavior {
        if (section.isList(key)) {
            return ActionBehavior.Simple(PlaceholderRegistry.resolve(section.getStringList(key)))
        }

        if (section.isString(key)) {
            val singleString = section.getString(key)!!
            return ActionBehavior.Simple(listOf(PlaceholderRegistry.resolve(singleString)))
        }

        if (section.isConfigurationSection(key)) {
            val sub = section.getConfigurationSection(key)!!
            if (sub.contains("when")) {
                val whenSec = sub.getConfigurationSection("when")!!
                val rawValue = whenSec.getString("value") ?: ""
                val valueStr = PlaceholderRegistry.resolve(rawValue)

                val resultSec = whenSec.getConfigurationSection("result")
                val resultMap = mutableMapOf<String, ActionBehavior>()

                if (resultSec != null) {
                    for (resKey in resultSec.getKeys(false)) {
                        resultMap[resKey] = parseBehavior(resultSec, resKey)
                    }
                }
                return ActionBehavior.When(valueStr, resultMap)
            }
        }
        return ActionBehavior.Simple(emptyList())
    }

    private fun flattenAndParse(section: ConfigurationSection, pathPrefix: String) {
        for (key in section.getKeys(false)) {
            val currentPath = if (pathPrefix.isEmpty()) key else "$pathPrefix.$key"

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

            if (subSection.contains("sequence")) {
                actionPacks[currentPath] = parseSequenceNode(subSection.getConfigurationSection("sequence")!!)
            } else if (isActionPack(subSection)) {
                actionPacks[currentPath] = parseStandardNode(subSection)
            } else {
                flattenAndParse(subSection, currentPath)
            }
        }
    }

    internal fun registerInline(section: ConfigurationSection): String? {
        val hasActions = section.getKeys(false).any {
            it.lowercase().contains("click") || it.lowercase() == "sequence"
        }

        if (!hasActions) return null

        val pack = if (section.contains("sequence")) {
            parseSequenceNode(section.getConfigurationSection("sequence")!!)
        } else {
            parseStandardNode(section)
        }

        val pathName = section.currentPath?.replace(".", "_") ?: "root"
        val uniqueHash = java.util.UUID.randomUUID().toString().take(6)
        val generatedId = "inline_${pathName}_$uniqueHash"

        actionPacks[generatedId] = pack
        return generatedId
    }

    private fun parseStandardNode(section: ConfigurationSection): GuiActionPack.Standard {
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

    private fun isActionPack(section: ConfigurationSection): Boolean {
        return section.getKeys(false).any {
            it.lowercase().contains("click") || it == "requirements" || it == "deny"
        }
    }
}