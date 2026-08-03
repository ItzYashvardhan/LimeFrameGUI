package net.justlime.limeframegui.registry.component

import org.bukkit.entity.Player

/**
 * Stores custom Kotlin conditions that can be evaluated in GUI requirements.
 * Syntax in YAML: [condition] custom_id arg1 arg2
 */
object ConditionRegistry {

    // Maps the condition identifier to its evaluation function
    private val conditions = mutableMapOf<String, (Player, List<String>) -> Boolean>()

    /**
     * Registers a custom condition.
     * @param id The identifier used in YAML (e.g., "check_warp_password")
     * @param executor A lambda returning true/false. Receives the Player and a List of arguments.
     */
    fun register(id: String, executor: (player: Player, args: List<String>) -> Boolean) {
        conditions[id.lowercase()] = executor
    }

    fun get(id: String): ((Player, List<String>) -> Boolean)? {
        return conditions[id.lowercase()]
    }

    /**
     * Clears registered conditions (usually called during a hard server reload).
     */
    fun clear() {
        conditions.clear()
    }
}