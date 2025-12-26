package net.justlime.limeframegui.utilities

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.regex.Pattern

object VersionHandler {


    private val SERVER_VERSION_PATTERN = Pattern.compile("\\(MC: ?(\\d+(\\.\\d+)*)\\)")

    fun getNativeServerVersion(): String {
        val matcher = SERVER_VERSION_PATTERN.matcher(Bukkit.getVersion())
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            Bukkit.getBukkitVersion().split("-", " ")[0]
        }
    }

    /**
     * Gets the client version.
     */
    fun getClientVersion(player: Player): String {
        if (Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            try {
                val protocolId = Via.getAPI().getPlayerVersion(player.uniqueId)
                val protocolVer = ProtocolVersion.getProtocol(protocolId)
                return protocolVer.name
            } catch (e: Exception) {
                // ViaVersion failed, ignore and use fallback
            }
        }
        return getNativeServerVersion()
    }

    /**
     * Checks if a player's client version is within a specified range.
     */
    fun isVersionSupported(player: Player, minVersion: String, maxVersion: String? = null): Boolean {
        val clientVersionStr = getClientVersion(player)
        val client = parseVersion(clientVersionStr)
        val min = parseVersion(minVersion)

        if (compareVersions(client, min) < 0) return false

        if (maxVersion != null) {
            val max = parseVersion(maxVersion)
            if (compareVersions(client, max) > 0) return false
        }
        return true
    }

    fun parseVersion(version: String): List<Int> {
        val parts = mutableListOf<Int>()
        val matcher = Pattern.compile("\\d+").matcher(version)
        while (matcher.find()) {
            matcher.group().toIntOrNull()?.let { parts.add(it) }
        }
        return parts
    }

    fun compareVersions(v1: List<Int>, v2: List<Int>): Int {
        val size = maxOf(v1.size, v2.size)
        for (i in 0 until size) {
            val part1 = v1.getOrElse(i) { 0 }
            val part2 = v2.getOrElse(i) { 0 }
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }
}