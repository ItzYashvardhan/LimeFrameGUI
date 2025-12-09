package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

data class LimeStyleSheet(
    var player: Player? = null,
    var offlinePlayer: OfflinePlayer? = null, //Recommended
    var placeholder: Map<String, String> = mutableMapOf(),
    var stylishTitle: Boolean = LimeFrameAPI.keys.stylishTitle,
    var stylishName: Boolean = LimeFrameAPI.keys.stylishName,
    var stylishLore: Boolean = LimeFrameAPI.keys.stylishLore,
) {

    fun isEmpty(): Boolean = player == null && offlinePlayer == null && placeholder.isEmpty() && stylishTitle == LimeFrameAPI.keys.stylishTitle && stylishName == LimeFrameAPI.keys.stylishName && stylishLore == LimeFrameAPI.keys.stylishLore

}
