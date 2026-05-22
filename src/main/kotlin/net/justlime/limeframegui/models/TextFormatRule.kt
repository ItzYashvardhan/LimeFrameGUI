package net.justlime.limeframegui.models

import net.justlime.limeframegui.enums.TextCase

// 🌟 2. The Format Rule (Font is boolean, Color removed, wrapLength added)
data class TextFormatRule(
    var font: Boolean = false,
    var weights: List<String> = emptyList(),
    var prefix: String = "",
    var suffix: String = "",
    var textCase: TextCase = TextCase.REGULAR,
    var wrapLength: Int = -1 // -1 means disabled
) {
    fun clone() = TextFormatRule(font, weights.toList(), prefix, suffix, textCase, wrapLength)
}