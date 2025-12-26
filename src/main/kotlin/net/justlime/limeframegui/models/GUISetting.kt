package net.justlime.limeframegui.models

data class GUISetting(
    var rows: Int,//
    var title: String,//
    var style: GuiStyleSheet = GuiStyleSheet(),//
    var label: String = ""
) {
    fun clone(): GUISetting {
        return GUISetting(rows, title, style.copy(), label)
    }
}