package net.justlime.limeframegui.util

import net.justlime.limeframegui.enums.AnsiColor
import net.justlime.limeframegui.util.LimeConsole.Companion.isLegacyEnvironment
import net.justlime.limeframegui.util.LimeConsole.Companion.registry
import org.bukkit.Bukkit

/**
 * A highly scalable console logger allowing developers to define their own branding.
 * Instances are managed via the companion object registry for global static access.
 */
class LimeConsole(
    private val pluginName: String,
    private val tagline: String = "",
    private val width: Int = 62,
    private val primaryColor: AnsiColor = AnsiColor.ORANGE,
    private val secondaryColor: AnsiColor = AnsiColor.CYAN,
    forceLegacy: Boolean? = null
) {

    private val isLegacy = forceLegacy ?: isLegacyEnvironment

    private val borderTopL = if (isLegacy) "+" else "┌"
    private val borderTopR = if (isLegacy) "+" else "┐"
    private val borderBotL = if (isLegacy) "+" else "└"
    private val borderBotR = if (isLegacy) "+" else "┘"
    private val borderMidL = if (isLegacy) "+" else "├"
    private val borderMidR = if (isLegacy) "+" else "┤"
    private val borderHorz = if (isLegacy) "-" else "─"
    private val borderVert = if (isLegacy) "|" else "│"

    private val safeWidth = maxOf(width, pluginName.length + tagline.length + 10)

    private fun stripAnsi(input: String): String {
        return input.replace(Regex("\u001B\\[[;\\d]*m"), "")
    }

    private fun wrapText(text: String, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (stripAnsi(currentLine.toString()).length + stripAnsi(word).length + 1 > maxWidth) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    fun printHeader() {
        val topBorder = borderTopL + borderHorz.repeat(safeWidth) + borderTopR

        val titleDisplay = "${AnsiColor.RESET}${AnsiColor.BOLD}$primaryColor $pluginName "
        val taglineDisplay =
            if (tagline.isNotBlank()) "${AnsiColor.RESET}${AnsiColor.GRAY}- $secondaryColor$tagline${AnsiColor.RESET}" else ""

        val rawTitleLength = stripAnsi(titleDisplay + taglineDisplay).length
        val padding = " ".repeat(maxOf(0, safeWidth - rawTitleLength))

        val midBorder = borderMidL + borderHorz.repeat(safeWidth) + borderMidR

        Bukkit.getConsoleSender().sendMessage(topBorder)
        Bukkit.getConsoleSender().sendMessage("$borderVert$titleDisplay$taglineDisplay$padding$borderVert")
        Bukkit.getConsoleSender().sendMessage(midBorder)
    }

    fun printStep(message: String, indicatorColor: AnsiColor = AnsiColor.BRIGHT_CYAN, indicatorSymbol: String = "»") {
        if (isLegacy) {
            Bukkit.getConsoleSender().sendMessage(message)
            return
        }

        val prefix = "${AnsiColor.RESET} ${AnsiColor.GRAY}$indicatorColor$indicatorSymbol "
        val prefixLength = stripAnsi(prefix).length
        val maxContentWidth = safeWidth - prefixLength - 2

        val lines = wrapText(message, maxContentWidth)

        lines.forEachIndexed { i, line ->
            val content =
                if (i == 0) "$prefix$line${AnsiColor.RESET}" else "${AnsiColor.RESET}   $line${AnsiColor.RESET}"
            val contentLen = stripAnsi(content).length
            val padding = " ".repeat(maxOf(0, safeWidth - contentLen))

            Bukkit.getConsoleSender().sendMessage("$borderVert$content$padding$borderVert")
        }
    }

    fun printBlock(messages: List<String>, indicatorColor: AnsiColor = AnsiColor.BRIGHT_CYAN) {
        printHeader()
        messages.forEach { printStep(it, indicatorColor) }
        printFooter()
    }

    fun printWarning(message: String) {
        val finalMessage = "[Warning] + $message"
        printStep(finalMessage, AnsiColor.YELLOW, "»")
    }


    fun printFooter() {
        Bukkit.getConsoleSender().sendMessage(borderBotL + borderHorz.repeat(safeWidth) + borderBotR + AnsiColor.RESET)
    }

    // =========================================================================
    // REGISTRY & COMPANION OBJECT
    // =========================================================================
    companion object {
        private val registry = mutableMapOf<String, LimeConsole>()

        /**
         * Smart detection for legacy consoles.
         * Checks if the console supports UTF-8 and isn't running in a basic IDE terminal.
         */
        val isLegacyEnvironment: Boolean by lazy {
            try {
                val encoding = System.getProperty("file.encoding", "UTF-8")
                val isUtf8 = encoding.equals("UTF-8", ignoreCase = true) || encoding.equals("UTF8", ignoreCase = true)

                val isIntelliJ = System.getProperty("java.class.path")?.contains("idea_rt.jar") == true

                val isWindows = System.getProperty("os.name").lowercase().contains("win")
                val isModernTerminal = System.getenv("WT_SESSION") != null // Windows Terminal sets this

                !isUtf8 || isIntelliJ || (isWindows && !isModernTerminal)
            } catch (_: Exception) {
                true
            }
        }

        fun register(id: String, console: LimeConsole) {
            registry[id] = console
        }

        fun get(id: String): LimeConsole {
            return registry[id] ?: throw IllegalArgumentException("[LimeFrameGUI] No console registered for ID: '$id'")
        }
    }
}