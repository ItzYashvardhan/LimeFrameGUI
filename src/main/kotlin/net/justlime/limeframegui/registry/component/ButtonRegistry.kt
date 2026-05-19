package net.justlime.limeframegui.registry.component

import net.justlime.limeframegui.event.GuiEventHandler
import org.bukkit.entity.Player

object ButtonRegistry {
    
    // Stores the identifier and the block of code to run
    private val codeActions = mutableMapOf<String, (Player, GuiEventHandler) -> Unit>()

    /**
     * API Method: Registers a custom block of code to an identifier.
     * @param identifier The string used in the YAML (e.g., "wipe_team_admin_logic")
     * @param action The code to execute when the tag is clicked
     */
    fun register(identifier: String, action: (Player, GuiEventHandler) -> Unit) {
        codeActions[identifier] = action
    }

    /**
     * Internal Method: Runs the registered code. Returns true if found.
     */
    fun execute(identifier: String, player: Player, gui: GuiEventHandler): Boolean {
        val action = codeActions[identifier]
        if (action != null) {
            action.invoke(player, gui)
            return true
        }
        return false
    }
}