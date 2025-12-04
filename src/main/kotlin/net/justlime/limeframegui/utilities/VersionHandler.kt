package net.justlime.limeframegui.utilities

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.regex.Pattern

object VersionHandler {

    private val SERVER_VERSION_PATTERN = Pattern.compile("\\(MC: (\\d+\\.\\d+(\\.\\d+)?)\\)")

    /**
     * Gets the native Minecraft version of the server.
     */
    fun getNativeServerVersion(): String {
        val matcher = SERVER_VERSION_PATTERN.matcher(Bukkit.getVersion())
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            Bukkit.getBukkitVersion().split("-")[0]
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
        // This will now never be null
        val clientVersionStr = getClientVersion(player)

        val clientVersion = parseVersion(clientVersionStr)
        val min = parseVersion(minVersion)

        // Check: Client >= Min
        val isAtLeastMin = compareVersions(clientVersion, min) >= 0

        if (!isAtLeastMin) return false

        // Check: Client <= Max (if max exists)
        return if (maxVersion != null) {
            val max = parseVersion(maxVersion)
            compareVersions(clientVersion, max) <= 0
        } else {
            true
        }
    }

    fun parseVersion(version: String): List<Int> {
        // Handle "1.16.x" or "Unknown" gracefully by filtering strictly for digits
        return version.replace(Regex("[^0-9.]"), "").split('.').mapNotNull { it.toIntOrNull() }
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