package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.manager.SessionManager
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.registry.component.LangRegistry
import net.justlime.limeframegui.registry.component.PlaceholderRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object TextResolver {

    private val varRegex = Regex("\\{var:([a-zA-Z0-9_.-]+)\\}")
    private val exactLangRegex = Regex("^\\{lang[.:]([a-zA-Z0-9_.-]+)\\}$")

    private val isPapiEnabled by lazy {
        Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
    }

    fun resolve(player: Player, text: String, setting: IContextSetting, itemStyle: GuiStyleSheet? = null): String {
        if (text.isEmpty()) return text

        var resolved = text

        // Determine the correct offline player and merged placeholders
        val targetOfflinePlayer = itemStyle?.offlinePlayer ?: setting.style.offlinePlayer
        val mergedPlaceholders = if (itemStyle != null) {
            setting.localPlaceholders + itemStyle.placeholder
        } else {
            setting.localPlaceholders
        }

        // Step 1: Resolve Lang files
        resolved = LangRegistry.resolveLangString(player, resolved, player.locale)



        // Step 2: Resolve Placeholders with the merged map
        if (resolved.contains("{player}")) {
            val targetName = targetOfflinePlayer?.name ?: player.name
            resolved = resolved.replace("{player}", targetName ?: "")
        }
        resolved = PlaceholderRegistry.resolve(resolved, mergedPlaceholders, player)

        // Step 3: Resolve Session Variables
        if (resolved.contains("{var:")) {
            resolved = varRegex.replace(resolved) { match ->
                val variableName = match.groupValues[1]
                SessionManager.getVariable(player, variableName, setting)
            }
        }

        // Step 4: Resolve PlaceholderAPI using OfflinePlayer if available!
        if (isPapiEnabled) {
            resolved = if (targetOfflinePlayer != null) {
                PlaceholderAPI.setPlaceholders(targetOfflinePlayer, resolved)
            } else {
                PlaceholderAPI.setPlaceholders(player, resolved)
            }
        }

        return resolved
    }

    fun resolveList(player: Player, list: List<String>, setting: IContextSetting, itemStyle: GuiStyleSheet? = null): List<String> {
        if (list.isEmpty()) return list

        val resultList = ArrayList<String>()

        for (line in list) {
            val trimmedLine = line.trim()

            if (trimmedLine.startsWith("lang.")) {
                val key = trimmedLine.substringAfter("lang.")
                val listResult = LangRegistry.getList(key, player.locale)
                if (listResult.isNotEmpty()) {
                    resultList.addAll(listResult.map { resolve(player, it, setting, itemStyle) })
                    continue
                }
            }

            val exactMatch = exactLangRegex.matchEntire(trimmedLine)
            if (exactMatch != null) {
                val key = exactMatch.groupValues[1]
                val listResult = LangRegistry.getList(key, player.locale)

                if (listResult.isNotEmpty()) {
                    resultList.addAll(listResult.map { resolve(player, it, setting, itemStyle) })
                    continue
                }
            }

            resultList.add(resolve(player, line, setting, itemStyle))
        }

        return resultList
    }
}