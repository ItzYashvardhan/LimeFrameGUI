package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI

data class GuiTextSettings(
    var title: TextFormatRule = TextFormatRule(),
    var name: TextFormatRule = TextFormatRule(),
    var lore: TextFormatRule = TextFormatRule()
) {
    fun isEmpty(): Boolean {
        return title == TextFormatRule() &&
                name == TextFormatRule() &&
                lore == TextFormatRule()
    }

    fun clone() = GuiTextSettings(title.clone(), name.clone(), lore.clone())
}