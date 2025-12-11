package net.justlime.limeframegui.utilities

import com.google.common.primitives.Ints
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerProfile
import java.net.URL
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object SkullUtils {


    /**
     * Applies a pre-created profile object to a SkullMeta.
     *
     * @param meta The SkullMeta to modify.
     * @param profileObject The profile object (either a PlayerProfile or GameProfile) to apply.
     */
    fun applySkin(meta: SkullMeta, profileObject: Any) {
        if (VersionHelper.HAS_PLAYER_PROFILES) {
            if (profileObject is PlayerProfile) {
                meta.ownerProfile = profileObject
            }
            return
        }

        // --- LEGACY (pre-1.18) ---
        if (profileObject is GameProfile) {
            try {
                val profileField = meta.javaClass.getDeclaredField("profile")
                profileField.isAccessible = true
                profileField.set(meta, profileObject)
            } catch (e: Exception) {
                Bukkit.getLogger().warning("[LimeFrameGUI] Failed to apply legacy skull texture via reflection: ${e.message}")
            }
        }
    }

    fun createLegacyGameProfile(texture: String): GameProfile {
        val profile = GameProfile(UUID.randomUUID(), null)
        val skinUrl = getSkinUrl(texture)

        if (skinUrl.isNotEmpty()) {
            val base64 = Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"$skinUrl\"}}}".toByteArray())
            profile.properties.put("textures", Property("textures", base64))
        }

        return profile
    }

    fun createProfileFromTexture(texture: String): PlayerProfile {
        val profile = Bukkit.createPlayerProfile(UUID.randomUUID())
        val textures = profile.textures
        val skinUrl = getSkinUrl(texture)

        if (skinUrl.isNotEmpty()) {
            runCatching {
                textures.skin = URL(skinUrl)
            }.onFailure {
                Bukkit.getLogger().warning("[LimeFrameGUI] Could not set skull skin from a malformed URL: $skinUrl. Error: ${it.message}")
            }
        }

        profile.setTextures(textures)
        return profile
    }

    /**
     * A helper function to resolve a texture string into a full Minecraft texture URL.
     */
    private fun getSkinUrl(texture: String): String {
        if (texture.startsWith("http")) {
            return texture
        }
        // Try to decode as Base64 first. If it returns null, assume it's a raw texture ID.
        return decodeSkinUrl(texture) ?: "https://textures.minecraft.net/texture/$texture"
    }

    object VersionHelper {
        const val V1_18_1: Int = 1181
        const val V1_12: Int = 1120

        val CURRENT_VERSION: Int = getCurrentVersion()
        val HAS_PLAYER_PROFILES: Boolean = CURRENT_VERSION >= V1_18_1
        val IS_SKULL_OWNER_LEGACY: Boolean = CURRENT_VERSION <= V1_12

        private fun getCurrentVersion(): Int {
            val matcher: Matcher = Pattern.compile("(?<version>\\d+\\.\\d+)(?<patch>\\.\\d+)?").matcher(Bukkit.getBukkitVersion())
            val stringBuilder = StringBuilder()
            if (matcher.find()) {
                stringBuilder.append(matcher.group("version").replace(".", ""))
                val patch: String? = matcher.group("patch")
                if (patch == null) stringBuilder.append("0")
                else stringBuilder.append(patch.replace(".", ""))
            }
            return Ints.tryParse(stringBuilder.toString()) ?: throw RuntimeException("Could not retrieve server version!")
        }
    }

    private val GSON = Gson()

    fun getTextureFromSkull(item: ItemStack): String? {
        val meta = item.itemMeta as? SkullMeta ?: return null
        if (VersionHelper.HAS_PLAYER_PROFILES) {
            val profile = meta.ownerProfile ?: return null
            val url = profile.textures.skin ?: return null
            return url.toString().removePrefix("https://textures.minecraft.net/texture/")
        }
        val profile = try {
            val profileField = meta.javaClass.getDeclaredField("profile")
            profileField.isAccessible = true
            profileField.get(meta) as? GameProfile
        } catch (e: Exception) {
            null
        } ?: return null
        val property = profile.properties.get("textures").firstOrNull() ?: return null
        return decodeSkinUrl(property.value)
    }

    fun decodeSkinUrl(base64Texture: String): String? {
        return try {
            val decodedBytes = Base64.getDecoder().decode(base64Texture)
            if (decodedBytes.isEmpty()) return null
            val decodedJson = String(decodedBytes)
            if (!decodedJson.trim().startsWith("{")) return null
            val decodedObject = GSON.fromJson(decodedJson, JsonObject::class.java)
            val textures = decodedObject["textures"]?.asJsonObject ?: return null
            val skin = textures["SKIN"]?.asJsonObject ?: return null
            skin["url"]?.asString
        } catch (e: Exception) {
            null
        }
    }
}