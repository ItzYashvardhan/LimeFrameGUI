package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

data class GuiStyleSheet(
    var viewer: Player? = null, //This will always be populated during opening a gui
    var offlinePlayer: OfflinePlayer? = null, //This is used to set placeholder value if none given it will take viewer as default
    var placeholder: Map<String, String> = mutableMapOf(),
    var stylishTitle: Boolean = LimeFrameAPI.keys.stylishTitle,
    var stylishName: Boolean = LimeFrameAPI.keys.stylishName,
    var stylishLore: Boolean = LimeFrameAPI.keys.stylishLore,
    var clickSound: GuiSound = LimeFrameAPI.keys.clickSound,
    var openSound: GuiSound = LimeFrameAPI.keys.openSound,
    var closeSound: GuiSound = LimeFrameAPI.keys.closeSound
) {

    fun isEmpty(): Boolean = viewer == null && offlinePlayer == null && placeholder.isEmpty() && stylishTitle == LimeFrameAPI.keys.stylishTitle && stylishName == LimeFrameAPI.keys.stylishName && stylishLore == LimeFrameAPI.keys.stylishLore && clickSound == LimeFrameAPI.keys.clickSound && openSound == LimeFrameAPI.keys.openSound && closeSound == LimeFrameAPI.keys.closeSound


}
