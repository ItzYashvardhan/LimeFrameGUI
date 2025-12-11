package net.justlime.limeframegui.utilities

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

object SkinFileHook {

    private val GSON = Gson()

    private val playersFolder: File by lazy { File(Bukkit.getUpdateFolderFile().parentFile, "SkinsRestorer/players") }

    private val skinsFolder: File by lazy { File(Bukkit.getUpdateFolderFile().parentFile, "SkinsRestorer/skins") }

    fun getSkinByUUID(uuid: UUID): String? {

        if (!playersFolder.exists()) return null

        var playerFile = File(playersFolder, "$uuid.player")

        if (!playerFile.exists()) {
            val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
            val name = offlinePlayer.name ?: return null
            val offlineUUID = UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(StandardCharsets.UTF_8))
            playerFile = File(playersFolder, "$offlineUUID.player")
        }

        if (!playerFile.exists()) return null

        val (identifier, type) = try {
            val content = playerFile.readText(StandardCharsets.UTF_8)
            val json = GSON.fromJson(content, JsonObject::class.java)
            val skinObj = json.getAsJsonObject("skinIdentifier") ?: return null

            val id = skinObj.get("identifier")?.asString ?: return null
            val type = skinObj.get("type")?.asString ?: "PLAYER"

            Pair(id, type)
        } catch (e: Exception) {
            return null
        }

        val extension = if (type.equals("CUSTOM", ignoreCase = true)) ".customskin" else ".playerskin"

        val skinFile = File(skinsFolder, "$identifier$extension")
        if (!skinFile.exists()) return null

        return try {
            val content = skinFile.readText(StandardCharsets.UTF_8)
            val json = GSON.fromJson(content, JsonObject::class.java)
            json.get("value")?.asString
        } catch (e: Exception) {
            null
        }
    }
}