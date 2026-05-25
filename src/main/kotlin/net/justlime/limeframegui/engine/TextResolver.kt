package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.manager.SessionManager
import net.justlime.limeframegui.registry.component.LangRegistry
import net.justlime.limeframegui.registry.component.PlaceholderRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object TextResolver {

    // Pre-compiled regex for ultra-fast session variable matching
    private val varRegex = Regex("\\{var:([a-zA-Z0-9_.-]+)\\}")

    // Matches {lang.key} or {lang:key} precisely to trigger List Expansion
    private val exactLangRegex = Regex("^\\{lang[.:]([a-zA-Z0-9_.-]+)\\}$")

    private val isPapiEnabled by lazy {
        Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
    }

    /**
     * The master text pipeline for single Strings (Titles, Names, Inline text).
     */
    fun resolve(player: Player, text: String, setting: IContextSetting): String {
        if (text.isEmpty()) return text

        var resolved = text

        // Step 1: Resolve Lang files
        resolved = LangRegistry.resolveLangString(player,resolved, player.locale)

        // Step 2: Resolve Placeholders (Local overrides Global)
        resolved = PlaceholderRegistry.resolve(resolved, setting.localPlaceholders,player)

        // Step 3: Resolve Session Variables (e.g. "{var:filter_state}")
        if (resolved.contains("{var:")) {
            resolved = varRegex.replace(resolved) { match ->
                val variableName = match.groupValues[1]
                SessionManager.getVariable(player, variableName, setting)
            }
        }

        // Step 4: Resolve PlaceholderAPI (e.g. "%player_name%")
        if (isPapiEnabled) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved)
        }

        return resolved
    }

    /**
     * Resolves an entire list of strings, properly expanding Localized Lists!
     */
    fun resolveList(player: Player, list: List<String>, setting: IContextSetting): List<String> {
        if (list.isEmpty()) return list

        val resultList = ArrayList<String>()

        for (line in list) {
            val trimmedLine = line.trim()

            // Check A: Is it the old exact syntax? (e.g., "lang.info_lore")
            if (trimmedLine.startsWith("lang.")) {
                val key = trimmedLine.substringAfter("lang.")
                val listResult = LangRegistry.getList(key, player.locale)
                if (listResult.isNotEmpty()) {
                    // Resolve variables/PAPI on the expanded lines before adding them!
                    resultList.addAll(listResult.map { resolve(player, it, setting) })
                    continue
                }
            }

            // Check B: Is the line EXACTLY a bracketed placeholder? (e.g., "{lang.info_lore}" or "{lang:info_lore}")
            val exactMatch = exactLangRegex.matchEntire(trimmedLine)
            if (exactMatch != null) {
                val key = exactMatch.groupValues[1]
                val listResult = LangRegistry.getList(key, player.locale)

                // If it resolves to a list, expand the list right here
                if (listResult.isNotEmpty()) {
                    resultList.addAll(listResult.map { resolve(player, it, setting) })
                    continue
                }
            }

            // Check C: Normal line processing (inline placeholders, variables, PAPI)
            resultList.add(resolve(player, line, setting))
        }

        return resultList
    }
}