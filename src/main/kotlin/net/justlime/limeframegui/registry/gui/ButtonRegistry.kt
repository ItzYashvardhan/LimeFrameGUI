package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.models.response.ActionTagRegistryResponse

object ButtonRegistry {

    // Stores the identifier and the block of code to run
    private val codeActions = mutableMapOf<String, (response: ActionTagRegistryResponse) -> Unit>()

    /**
     * API Method: Registers a custom block of code to an identifier.
     * @param identifier The string used in the YAML (e.g., "wipe_team_admin_logic")
     * @param action The code to execute when the tag is clicked
     */
    fun register(identifier: String, response: (ActionTagRegistryResponse) -> Unit) {
        codeActions[identifier] = response
    }

    /**
     * Internal Method: Runs the registered code. Returns true if found.
     */
    internal fun execute(identifier: String, response: ActionTagRegistryResponse): Boolean {
        val action = codeActions[identifier]
        if (action != null) {
            action.invoke(response)
            return true
        }
        return false
    }
}