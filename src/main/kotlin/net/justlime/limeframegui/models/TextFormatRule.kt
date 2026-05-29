package net.justlime.limeframegui.models

import net.justlime.limeframegui.enums.TextCase

data class TextFormatRule(
    var font: Boolean? = null,
    var weights: List<String>? = null,
    var prefix: String? = null,
    var suffix: String? = null,
    var textCase: TextCase? = null,
    var wrapLength: Int? = null
) {
    fun clone() = TextFormatRule(
        font,
        weights?.toList(),
        prefix,
        suffix,
        textCase,
        wrapLength
    )
}