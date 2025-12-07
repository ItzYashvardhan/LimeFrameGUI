package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

data class GUISetting(
    var rows: Int,
    var title: String,
    var placeholderPlayer: Player? = null,
    var placeholderOfflinePlayer: OfflinePlayer? = null,
    var customPlaceholder: Map<String, String> = mutableMapOf(),
    var smallCapsTitle: Boolean? = LimeFrameAPI.keys.smallCaps,
    var smallCapsItemName: Boolean? = LimeFrameAPI.keys.smallCaps,
    var smallCapsItemLore: Boolean? = LimeFrameAPI.keys.smallCaps,
    var type: InventoryType = InventoryType.CHEST,
)