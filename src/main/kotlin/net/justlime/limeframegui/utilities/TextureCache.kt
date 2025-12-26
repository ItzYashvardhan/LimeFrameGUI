package net.justlime.limeframegui.utilities

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory cache to prevent hitting Mojang API every time the menu opens.
 */
object TextureCache {
    private val cache = ConcurrentHashMap<UUID, String>()

    fun get(uuid: UUID): String? = cache[uuid]
    fun add(uuid: UUID, texture: String) { cache[uuid] = texture }
}

/**
 * Fetches skin texture value directly from Mojang Session Server.
 */
object MojangTextureFetcher {
    fun fetch(uuid: UUID): String? {
        return try {
            val url = URL("https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false")
            val reader = InputStreamReader(url.openStream())
            val json = JsonParser.parseReader(reader).asJsonObject

            // Navigate JSON: properties -> [0] -> value
            val properties = json.getAsJsonArray("properties")
            if (properties.size() > 0) {
                properties.get(0).asJsonObject.get("value").asString
            } else {
                null
            }
        } catch (e: Exception) {
            null // Failed to fetch (Rate limit or invalid UUID)
        }
    }
}