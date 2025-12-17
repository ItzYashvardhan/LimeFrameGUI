package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

data class GuiStyleSheet(
    var player: Player? = null,
    var offlinePlayer: OfflinePlayer? = null, //Recommended
    var placeholder: Map<String, String> = mutableMapOf(),
    var stylishTitle: Boolean = LimeFrameAPI.keys.stylishTitle,
    var stylishName: Boolean = LimeFrameAPI.keys.stylishName,
    var stylishLore: Boolean = LimeFrameAPI.keys.stylishLore,
    var clickSound: GuiSound = LimeFrameAPI.keys.clickSound,
    var openSound: GuiSound = LimeFrameAPI.keys.openSound,
    var closeSound: GuiSound = LimeFrameAPI.keys.closeSound
) {

    fun isEmpty(): Boolean = player == null && offlinePlayer == null && placeholder.isEmpty() && stylishTitle == LimeFrameAPI.keys.stylishTitle && stylishName == LimeFrameAPI.keys.stylishName && stylishLore == LimeFrameAPI.keys.stylishLore && clickSound == LimeFrameAPI.keys.clickSound && openSound == LimeFrameAPI.keys.openSound && closeSound == LimeFrameAPI.keys.closeSound


}
