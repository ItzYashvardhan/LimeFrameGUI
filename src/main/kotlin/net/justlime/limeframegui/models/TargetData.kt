package net.justlime.limeframegui.models

import org.bukkit.OfflinePlayer

data class TargetData(
    val player: OfflinePlayer,
    val forwardedPlaceholder: Map<String, String> = emptyMap()
)
