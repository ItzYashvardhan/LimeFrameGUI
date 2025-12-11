package net.justlime.limeframegui.integration

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This object provides a hook into the SkinsRestorer plugin's data storage
 * to retrieve player skin textures. It supports both file-based and database-based
 * SkinsRestorer configurations.
 */
object SkinRestorerHook {

    // Stores UUID -> Base64 Texture
    private val textureCache = ConcurrentHashMap<UUID, String>()

    //File
    private val GSON = Gson()
    private val playersFolder: File by lazy { File(Bukkit.getUpdateFolderFile().parentFile, "SkinsRestorer/players") }
    private val skinsFolder: File by lazy { File(Bukkit.getUpdateFolderFile().parentFile, "SkinsRestorer/skins") }

    //Database
    private var connection: Connection? = null
    private var tablePrefix = "sr_"
    private var jdbcUrl = ""
    private var user = ""
    private var password = ""
    private var isDatabaseEnabled = false

    fun init() {
        val pluginFolder = File(Bukkit.getUpdateFolderFile().parentFile, "SkinsRestorer")
        val configFile = File(pluginFolder, "config.yml")

        if (!configFile.exists()) {
            return
        }

        // Read their config.yml directly
        val config = YamlConfiguration.loadConfiguration(configFile)

        // Check if database is enabled in their config
        if (!config.getBoolean("database.enabled")) {
            return
        }

        val host = config.getString("database.host")
        val port = config.getInt("database.port")
        val dbName = config.getString("database.database")
        var options = config.getString("database.connectionOptions", "sslMode=disable")

        // Replace 'trust' (old/MariaDB style) with 'PREFERRED' (Modern MySQL style)
        if (options!!.contains("sslMode=trust")) {
            options = options.replace("sslMode=trust", "sslMode=PREFERRED")
        }
        this.user = config.getString("database.username", "root")!!
        this.password = config.getString("database.password", "")!!
        this.tablePrefix = config.getString("database.tablePrefix", "sr_")!!

        // Construct JDBC URL (MySQL)
        this.jdbcUrl = "jdbc:mysql://$host:$port/$dbName?$options"
        this.isDatabaseEnabled = true

        // Test connection
        ensureConnection()
    }

    fun getSkin(uuid: UUID): String? {
        if (textureCache.containsKey(uuid)) return textureCache[uuid]
        val texture = fetchFromSource(uuid)
        if (texture != null) textureCache[uuid] = texture
        return texture
    }

    fun cache(uuid: UUID, textureValue: String?) {
        if (textureValue == null) textureCache.remove(uuid)
        else textureCache[uuid] = textureValue
    }

    /**
     * Attempts to retrieve a skin texture for a given UUID,
     * prioritizing the database hook, then falling back to the file hook.
     *
     * @param uuid The UUID of the player.
     * @return The base64 encoded skin texture string, or null if not found.
     */
    @Synchronized
    private fun fetchFromSource(uuid: UUID): String? = if (isDatabaseEnabled) getSkinByUUIDFromDatabase(uuid) else getSkinByUUIDFromFile(uuid)

    /**
     * Retrieves the Skin Texture from the File System.
     */
    private fun getSkinByUUIDFromFile(uuid: UUID): String? {

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

    /**
     * Retrieves the Skin Texture from the Database using the logic from MySQLAdapter.java.
     */
    fun getSkinByUUIDFromDatabase(uuid: UUID): String? {
        if (!isDatabaseEnabled) return null

        ensureConnection()

        return try {
            // Try Online UUID first, then Offline UUID (Cracked fallback)
            var playerData = fetchPlayerData(uuid.toString())
            if (playerData == null) {
                val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
                val name = offlinePlayer.name ?: return null
                val offlineUUID = UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8))
                playerData = fetchPlayerData(offlineUUID.toString())
            }

            if (playerData == null) return null // Player has no skin set

            val (identifier, type) = playerData

            // Fetch the Texture Value from the correct table
            when (type) {
                "PLAYER" -> fetchValueFromTable("${tablePrefix}player_skins", "uuid", identifier)
                "CUSTOM" -> fetchValueFromTable("${tablePrefix}custom_skins", "name", identifier)
                "URL" -> fetchValueFromTable("${tablePrefix}url_skins", "url", identifier)
                else -> null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper: Queries the main players table.
     * @Synchronized ensures two threads don't crash the single connection
     */
    private fun fetchPlayerData(uuidString: String): Pair<String, String>? {
        // Just in case connection dropped, reconnect
        ensureConnection()

        val query = "SELECT skin_identifier, skin_type FROM ${tablePrefix}players WHERE uuid = ? LIMIT 1"

        return connection?.prepareStatement(query)?.use { stmt ->
            stmt.setString(1, uuidString)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val id = rs.getString("skin_identifier")
                    val type = rs.getString("skin_type")
                    if (id != null && type != null) Pair(id, type) else null
                } else null
            }
        }
    }

    private fun fetchValueFromTable(tableName: String, keyColumn: String, keyValue: String): String? {
        ensureConnection()

        val query = "SELECT value FROM $tableName WHERE $keyColumn = ? LIMIT 1"

        return connection?.prepareStatement(query)?.use { stmt ->
            stmt.setString(1, keyValue)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getString("value") else null
            }
        }
    }

    private fun ensureConnection() {
        try {
            if (connection == null || connection!!.isClosed || !connection!!.isValid(2)) {
                connection = DriverManager.getConnection(jdbcUrl, user, password)
            }
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[LimeFrameGUI] Failed to connect to SkinsRestorer Database: ${e.message}")
        }
    }

    fun close() {
        connection?.close()
    }
}