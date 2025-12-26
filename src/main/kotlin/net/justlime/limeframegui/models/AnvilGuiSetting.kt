package net.justlime.limeframegui.models

data class AnvilGuiSetting(
    val title: String,
    val label: String = "",
    val leftItem: GuiItem,
    val rightItem: GuiItem,
    val outPutItem: GuiItem,
    val openSound: GuiSound? = null,
    val cancelSound: GuiSound? = null,
    val submitSound: GuiSound? = null,
    val style: GuiStyleSheet,
    val preventClose: Boolean = false
)