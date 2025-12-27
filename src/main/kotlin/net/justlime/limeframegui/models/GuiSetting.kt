package net.justlime.limeframegui.models

data class GuiSetting(
    var rows: Int,//
    var title: String,//
    var style: GuiStyleSheet = GuiStyleSheet(),//
    var label: String = ""
) {
    fun clone(): GuiSetting {
        return GuiSetting(rows, title, style.copy(), label)
    }
}