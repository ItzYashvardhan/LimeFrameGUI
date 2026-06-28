package net.justlime.limeframegui.engine

import net.justlime.limeframegui.enums.TextCase
import net.justlime.limeframegui.models.TextFormatRule

object TextEngine {

    /**
     * Wraps the processed text in the prefix, suffix, and weights.
     */
    fun applyFormatTags(processedText: String, rule: TextFormatRule): String {
        val builder = StringBuilder()

        // Add Prefix (e.g. <gradient:blue:red> or <green>)
        if (!rule.prefix.isNullOrEmpty()) builder.append(rule.prefix)

        // Add Weights (e.g. <bold><underlined>)
        rule.weights?.forEach { weight ->
            builder.append("<${weight.lowercase()}>")
        }

        // Add the actual text
        builder.append(processedText)

        // Add Suffix (e.g. </gradient>)
        if (!rule.suffix.isNullOrEmpty()) builder.append(rule.suffix)

        return builder.toString()
    }

    /**
     * Applies casing rules to the text.
     */
    fun applyCasing(text: String, textCase: TextCase?): String {
        if (textCase == null || textCase == TextCase.REGULAR) return text

        val words = text.split(Regex("[_\\-\\s]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return text

        return when (textCase) {
            // "epic diamond sword" -> "EPIC DIAMOND SWORD"
            TextCase.UPPERCASE -> text.uppercase()

            // "epic diamond sword" -> "epic diamond sword"
            TextCase.LOWERCASE -> text.lowercase()

            // "epic diamond sword" -> "Epic Diamond Sword"
            TextCase.TITLE_CASE -> words.joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }

            // "epic diamond sword" -> "Epic diamond sword"
            TextCase.SENTENCE_CASE -> {
                val lower = text.lowercase()
                lower.replaceFirstChar { it.uppercase() }
            }

            // "epic diamond sword" -> "epicDiamondSword"
            TextCase.CAMEL_CASE -> words.mapIndexed { index, word ->
                if (index == 0) word.lowercase() else word.lowercase().replaceFirstChar { it.uppercase() }
            }.joinToString("")

            // "epic diamond sword" -> "EpicDiamondSword"
            TextCase.PASCAL_CASE -> words.joinToString("") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }

            // "epic diamond sword" -> "epic-diamond-sword"
            TextCase.SNAKE_CASE -> words.joinToString("_") { it.lowercase() }

            // "epic diamond sword" -> "epic-diamond-sword"
            TextCase.KEBAB_CASE -> words.joinToString("-") { it.lowercase() }

            TextCase.REGULAR -> text
        }
    }

    /**
     * Centers text perfectly for a standard 176-pixel Minecraft Chest GUI.
     * Calculates proportional pixel width, ignoring color/gradient tags.
     */
    fun applyCenter(text: String, rule: TextFormatRule): String {
        if (rule.center != true) return text

        val miniMessageRegex = Regex("<[^>]*>")
        val legacyRegex = Regex("(?i)[&§][0-9A-FK-ORX]")
        val rawVisibleText = text.replace(miniMessageRegex, "").replace(legacyRegex, "")

        var pixelWidth = 0
        val isBold = rule.weights?.any { it.equals("bold", ignoreCase = true) } == true

        // Calculate the exact pixel width of the text
        for (c in rawVisibleText) {
            if (c == '§') continue
            var charWidth = when (c) {
                '!', '.', ',', ':', ';', 'i', '|', '\'', 'ɪ' -> 2
                'l', '`', 'ʟ' -> 3
                ' ', 'I', '[', ']', 't', '"', 'ᴛ' -> 4
                'k', 'f', '<', '>', 'ᴋ', 'ғ' -> 5
                '@', '~' -> 7
                else -> 6 // Standard width for almost all A-Z, 0-9, and Small Caps
            }
            if (isBold && c != ' ') charWidth += 1 // Bold letters get 1px thicker
            pixelWidth += charWidth
        }

        // Chest title starts 8 pixels from the left edge. Center point is 88.
        val paddingPixels = 80 - (pixelWidth / 2)
        if (paddingPixels <= 0) return text // Text is too big to be centered

        // A standard space is 4 pixels wide.
        val spacesNeeded = paddingPixels / 4
        return " ".repeat(spacesNeeded) + text
    }
    /**
     * Automatically inserts newlines into long strings.
     * Intelligently ignores MiniMessage and Legacy color tags when calculating line length
     */
    fun applyWrap(text: String, wrapLength: Int?): String {
        if (wrapLength == null || wrapLength <= 0) return text

        // Regex to match MiniMessage tags <...> and Legacy tags §a, &a
        val miniMessageRegex = Regex("<[^>]*>")
        val legacyRegex = Regex("(?i)[&§][0-9A-FK-ORX]")

        // if the raw visible text is already short enough, skip processing
        val rawVisibleText = text.replace(miniMessageRegex, "").replace(legacyRegex, "")
        if (rawVisibleText.length <= wrapLength) return text

        val words = text.split(" ")
        val builder = StringBuilder()
        var currentLineLength = 0

        for (word in words) {
            // Strip the tags
            val visibleWordLength = word.replace(miniMessageRegex, "").replace(legacyRegex, "").length

            if (currentLineLength + visibleWordLength > wrapLength && currentLineLength > 0) {
                builder.append("\n") // Line is full, wrap it!
                currentLineLength = 0
            } else if (currentLineLength > 0) {
                builder.append(" ")
                currentLineLength++ // Count the space
            }

            // Append the REAL word (with its tags intact!)
            builder.append(word)
            currentLineLength += visibleWordLength
        }
        return builder.toString()
    }
}