package net.justlime.limeframegui.util

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
     * Gets the client's Minecraft version.
     * It prioritizes ViaVersion for accuracy on servers that support multiple client versions.
     * If ViaVersion is not available, it falls back to the server's version.
     */
    fun getClientVersion(player: Player): String {
        // Try to get client version from ViaVersion for best accuracy
        if (Bukkit.getPluginManager().isPluginEnabled("ViaVersion")) {
            try {
                val protocolId = Via.getAPI().getPlayerVersion(player.uniqueId)
                val protocolVer = ProtocolVersion.getProtocol(protocolId)
                // Successfully got version from ViaVersion
                return protocolVer.name
            } catch (e: Exception) {
                // ViaVersion is installed, but we failed to get the version.
                // This can happen, so we'll log it and fall back.
                Bukkit.getLogger().warning("[LimeFrameGUI] Failed to get player version from ViaVersion: ${e.message}")
            }
        }

        // If ViaVersion is not installed, or if it failed, fall back to the native server version.
        // This is less accurate on multi-version servers but ensures compatibility.
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
        // First, strip metadata like -SNAPSHOT, -R0.1, etc., to get the core version.
        var versionString = version.split("-")[0]

        // Handle different versioning schemes for standalone numbers (e.g. "19", "26")
        if (!versionString.contains(".") && versionString.matches(Regex("\\d+"))) {
            val num = versionString.toIntOrNull()
            // If the number is in the range of old Minecraft minor versions (e.g., 8 to 25), format it as 1.x
            if (num != null && num <= 25) {
                versionString = "1.$versionString"
            }
            // Otherwise, it's a new major version like "26", so we leave it as is.
        }

        val parts = mutableListOf<Int>()
        val matcher = Pattern.compile("\\d+").matcher(versionString)
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