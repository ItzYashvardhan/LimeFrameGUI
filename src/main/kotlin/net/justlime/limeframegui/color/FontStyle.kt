package net.justlime.limeframegui.color

import me.clip.placeholderapi.PlaceholderAPI
import net.justlime.limeframegui.engine.TextEngine
import net.justlime.limeframegui.enums.CapsState
import net.justlime.limeframegui.enums.ColorType
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.registry.component.FontRegistry
import net.justlime.limeframegui.util.VersionHandler
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

object FontStyle {
    private val fontCache = ConcurrentHashMap<String, Map<Char, String>>()
    private lateinit var colorType: ColorType
    var miniMessage: IMiniMessage? = null

    fun setColorType(color: ColorType) {
        colorType = color
    }

    fun initMiniMessage() {
        val mini = KyoriMiniMessage()
        try {
            miniMessage = mini
        } catch (e: Exception) {
            Bukkit.getLogger().warning(e.message)
        }
    }

    private val isPlaceholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null

    /**
     * - Apply color formatting based on the current ColorType.
     * Always returns a String.
     * - Priority to [OfflinePlayer] if both player type given
     */
    fun applyStyle(text: String, style: GuiStyleSheet, rule: TextFormatRule): String {
        var newText = text

        // Placeholders
        val playerName = style.offlinePlayer?.name
        newText = newText.customPlaceholder(playerName, style.placeholder)

        if (isPlaceholderAPIEnabled) {
            newText = when {
                style.offlinePlayer != null -> PlaceholderAPI.setPlaceholders(style.offlinePlayer, newText)
                style.viewer != null -> PlaceholderAPI.setPlaceholders(style.viewer, newText)
                else -> newText
            }
        }

        // Apply Casing and Word Wrap
        newText = TextEngine.applyCasing(newText, rule.textCase)
        newText = TextEngine.applyWrap(newText, rule.wrapLength)

        // Format Engine & Parsing
        val coloredText = when (colorType) {
            ColorType.LEGACY -> {
                ChatColor.translateAlternateColorCodes('&', newText)
            }

            ColorType.MINI_MESSAGE -> {
                // Translate old legacy to Kyori format
                newText = newText.replaceLegacyToMini()

                // Wrap the text in the prefix, suffix, and weights
                newText = TextEngine.applyFormatTags(newText, rule)

                try {
                    miniMessage?.legacyToMini(newText) ?: newText
                } catch (e: Exception) {
                    println(e.message)
                    newText
                }
            }
        }

        // Apply Small Caps Font
        return coloredText.toSmallCaps(style.viewer, rule.font)
    }

    /**
     * Processes a list of lore lines, handling line-wrapping color bleeds.
     */
    fun applyStyle(textList: List<String>, styleSheet: GuiStyleSheet, rule: TextFormatRule): List<String> {
        val processedList = mutableListOf<String>()

        for (line in textList) {
            val processedLine = applyStyle(line, styleSheet, rule)

            // If the wrapLength engine added a newline (\n), split it into separate lore lines!
            if (processedLine.contains("\n")) {
                val splits = processedLine.split("\n")
                var lastColors = ""

                for (i in splits.indices) {
                    val currentLine = splits[i]
                    if (i == 0) {
                        processedList.add(currentLine)
                        lastColors = extractLastColors(currentLine)
                    } else {
                        // 🌟 FIX: Prepend §r to kill Bukkit's default Italic Pink, then re-apply the last colors!
                        val restoredLine = "§r$lastColors$currentLine"
                        processedList.add(restoredLine)
                        lastColors = extractLastColors(restoredLine)
                    }
                }
            } else {
                processedList.add(processedLine)
            }
        }
        return processedList
    }

    private fun String.replaceLegacyToMini(): String {
        if (!this.contains('§') && !this.contains('&')) return this
        val builder = StringBuilder(this.length + 16)
        var i = 0
        while (i < this.length) {
            val c = this[i]
            if ((c == '§' || c == '&') && i + 1 < this.length) {
                val next = this[i + 1].lowercaseChar()
                val replacement = when (next) {
                    '0' -> "<black>"; '1' -> "<dark_blue>"; '2' -> "<dark_green>"
                    '3' -> "<dark_aqua>"; '4' -> "<dark_red>"; '5' -> "<dark_purple>"
                    '6' -> "<gold>"; '7' -> "<gray>"; '8' -> "<dark_gray>"
                    '9' -> "<blue>"; 'a' -> "<green>"; 'b' -> "<aqua>"
                    'c' -> "<red>"; 'd' -> "<light_purple>"; 'e' -> "<yellow>"
                    'f' -> "<white>"; 'l' -> "<bold>"; 'm' -> "<strikethrough>"
                    'n' -> "<underlined>"; 'o' -> "<italic>"; 'r' -> "<reset>"
                    else -> null
                }
                if (replacement != null) {
                    builder.append(replacement)
                    i += 2
                    continue
                }
            }
            builder.append(c)
            i++
        }
        return builder.toString()
    }

    private fun String.customPlaceholder(name: String?, customPlaceholders: Map<String, String>?): String {
        var result = this
        if (name != null) {
            result = result.replace("{player}", name)
        }
        customPlaceholders?.forEach { (key, value) -> result = result.replace(key, value) }
        return result
    }

    /**
     * Converts a string to small caps with advanced tag support.
     *
     * - Automatically selects the best font map for the viewer's version.
     * - Falls back to the server version if the viewer is null.
     * - Obeys <caps> and <no-caps> tags to override the default behavior.
     * Converts a string to small caps with advanced tag support.
     */
    fun String.toSmallCaps(viewer: Player?, useSmallCaps: Boolean?): String {
        if (useSmallCaps != true && FontRegistry.getFonts.isEmpty()) return this

        // 🌟 FIX: Grab the fully prepared map from the cache.
        // We completely removed the sorting block from here!
        val selectedFontMap = getFontMapCached(viewer)

        val result = StringBuilder()
        var i = 0
        var currentCapsState = CapsState.DEFAULT

        while (i < this.length) {
            val char = this[i]

            // Protect existing emojis and 4-byte characters
            if (char.isHighSurrogate()) {
                result.append(char)
                if (i + 1 < this.length) {
                    result.append(this[i + 1])
                    i++
                }
                i++
                continue
            }

            // Check for tags like <caps> or <no-caps>
            if (char == '<') {
                val closingIndex = this.indexOf('>', startIndex = i)
                if (closingIndex != -1) {
                    val tag = this.substring(i + 1, closingIndex)
                    when (tag.lowercase()) {
                        "caps" -> currentCapsState = CapsState.FORCE_ON
                        "/caps" -> currentCapsState = CapsState.DEFAULT
                        "no-caps" -> currentCapsState = CapsState.FORCE_OFF
                        "/no-caps" -> currentCapsState = CapsState.DEFAULT
                        else -> result.append(this, i, closingIndex + 1)
                    }
                    i = closingIndex + 1
                    continue
                }
            }

            val shouldConvert = when (currentCapsState) {
                CapsState.FORCE_ON -> true
                CapsState.FORCE_OFF -> false
                CapsState.DEFAULT -> useSmallCaps == true
            }

            if (shouldConvert && selectedFontMap != null) {
                when (char) {
                    '&', '§' -> { // Skip color codes
                        result.append(char)
                        if (i + 1 < this.length) {
                            result.append(this[i + 1])
                            i++
                        }
                    }

                    else -> {
                        // Swap character using the cached map
                        val replacement = selectedFontMap[char.lowercaseChar()]
                        if (replacement != null) {
                            result.append(replacement)
                        } else {
                            result.append(char)
                        }
                    }
                }
            } else {
                result.append(char)
            }
            i++
        }

        return result.toString()
    }

    /**
     * Extracts the last active color and formatting codes from a legacy string.
     * Fully supports modern Kyori Hex formats (§x§R§R§G§G§B§B).
     */
    private fun extractLastColors(text: String): String {
        var result = ""
        var index = 0
        while (index < text.length - 1) {
            if (text[index] == '§' || text[index] == '&') {
                val code = text[index + 1].lowercaseChar()
                if (code in "0123456789abcdef") {
                    result = "§$code"
                    index++
                } else if (code in "klmno") {
                    result += "§$code"
                    index++
                } else if (code == 'r') {
                    result = ""
                    index++
                } else if (code == 'x') {
                    // Modern Hex format check (requires 14 chars total: §x§1§2§3§4§5§6)
                    if (index + 13 < text.length) {
                        result = text.substring(index, index + 14)
                        index += 13
                    }
                }
            }
            index++
        }
        return result
    }

    private fun getFontMapCached(viewer: Player?): Map<Char, String>? {
        val fontMaps = FontRegistry.getFonts
        if (fontMaps.isEmpty()) return null

        val versionStr = viewer?.let { VersionHandler.getClientVersion(it) }
            ?: VersionHandler.getNativeServerVersion()

        return fontCache.getOrPut(versionStr) {
            val targetVersion = VersionHandler.parseVersion(versionStr)

            // Sort the versions to find the best match
            val bestVersionKey = fontMaps.keys.sortedWith { v1, v2 ->
                VersionHandler.compareVersions(
                    VersionHandler.parseVersion(v2),
                    VersionHandler.parseVersion(v1)
                )
            }.firstOrNull { versionKey ->
                VersionHandler.compareVersions(targetVersion, VersionHandler.parseVersion(versionKey)) >= 0
            }

            if (bestVersionKey == null) {
                null
            } else {
                val rawMap = fontMaps[bestVersionKey]
                val safeCharMap = mutableMapOf<Char, String>()

                // 🌟 BULLETPROOF MAPPING:
                // Converts the key to a String and grabs the 1st char.
                // Works 100% of the time, no matter what FontRegistry returns!
                rawMap?.forEach { (key, value) ->
                    val charKey = key.toString()[0]
                    safeCharMap[charKey] = value
                }

                safeCharMap // Save this into the cache
            }
        }
    }
}