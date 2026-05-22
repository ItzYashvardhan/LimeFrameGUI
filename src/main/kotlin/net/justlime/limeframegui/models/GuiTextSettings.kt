package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI

data class GuiTextSettings(
    var title: TextFormatRule = TextFormatRule(font = LimeFrameAPI.keys.stylishTitle),
    var name: TextFormatRule = TextFormatRule(font = LimeFrameAPI.keys.stylishName),
    var lore: TextFormatRule = TextFormatRule(font = LimeFrameAPI.keys.stylishLore)
) {
    fun isEmpty(): Boolean{
        return title == TextFormatRule(font = LimeFrameAPI.keys.stylishTitle) &&
                name == TextFormatRule(font = LimeFrameAPI.keys.stylishName) &&
                lore == TextFormatRule(font = LimeFrameAPI.keys.stylishLore)
    }
    fun clone() = GuiTextSettings(title.clone(), name.clone(), lore.clone())
}