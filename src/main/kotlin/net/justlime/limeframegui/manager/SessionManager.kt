package net.justlime.limeframegui.manager

import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.storage.IVariableStorage
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Orchestrates dynamic player state variables with a hierarchical fallback:
 * Runtime Cache -> GUI Local Variable -> Global Default.
 */
object SessionManager {

    private lateinit var storageProvider: IVariableStorage
    private val activeSessions = ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>>()

    // Replaces VariableRegistry. Load your global variables.yml into this map on startup.
    val globalDefaults = ConcurrentHashMap<String, String>()

    fun init(provider: IVariableStorage) {
        this.storageProvider = provider
    }

    fun loadSession(playerUuid: UUID) {
        storageProvider.loadAll(playerUuid).thenAccept { data ->
            val sessionMap = ConcurrentHashMap<String, String>()
            sessionMap.putAll(data)
            activeSessions[playerUuid] = sessionMap
        }
    }

    fun unloadSession(playerUuid: UUID) {
        activeSessions.remove(playerUuid)
    }

    fun getVariable(player: Player, varName: String, setting: IContextSetting): String {
        return activeSessions[player.uniqueId]?.get(varName)
            ?: setting.localVariables[varName]
            ?: globalDefaults[varName]
            ?: "UNKNOWN"
    }

    fun setVariable(player: Player, varName: String, value: String) {
        activeSessions.getOrPut(player.uniqueId) { ConcurrentHashMap() }[varName] = value
        storageProvider.saveValue(player.uniqueId, varName, value)
    }
}