package net.justlime.limeframegui.storage

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool

/**
 * Handles asynchronous read/write operations for player-specific variable states.
 */
class UUIDYamlStorage(plugin: Plugin) : IVariableStorage {
    
    private val folder = File(plugin.dataFolder, "gui/data/players")
    private val fileLocks = ConcurrentHashMap<UUID, Any>()

    init {
        if (!folder.exists()) folder.mkdirs()
    }

    private fun getLock(uuid: UUID) = fileLocks.getOrPut(uuid) { Any() }

    override fun loadValue(playerUuid: UUID, key: String): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync({
            synchronized(getLock(playerUuid)) {
                val file = File(folder, "$playerUuid.yml")
                if (!file.exists()) return@supplyAsync null
                return@supplyAsync YamlConfiguration.loadConfiguration(file).getString("variables.$key")
            }
        }, ForkJoinPool.commonPool())
    }

    override fun saveValue(playerUuid: UUID, key: String, value: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            synchronized(getLock(playerUuid)) {
                val file = File(folder, "$playerUuid.yml")
                val config = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()
                config.set("variables.$key", value)
                config.save(file)
            }
        }, ForkJoinPool.commonPool())
    }

    override fun loadAll(playerUuid: UUID): CompletableFuture<Map<String, String>> {
        return CompletableFuture.supplyAsync({
            synchronized(getLock(playerUuid)) {
                val file = File(folder, "$playerUuid.yml")
                if (!file.exists()) return@supplyAsync emptyMap<String, String>()
                
                val config = YamlConfiguration.loadConfiguration(file)
                val resultMap = mutableMapOf<String, String>()
                val sec = config.getConfigurationSection("variables") ?: return@supplyAsync emptyMap()
                
                for (key in sec.getKeys(false)) {
                    resultMap[key] = sec.getString(key) ?: ""
                }
                return@supplyAsync resultMap
            }
        }, ForkJoinPool.commonPool())
    }
}