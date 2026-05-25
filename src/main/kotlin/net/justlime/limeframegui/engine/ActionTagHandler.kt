package net.justlime.limeframegui.engine

import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.models.registry.ActionTagRegistryResponse
import org.bukkit.entity.Player

/**
 * The functional interface for handling action tags.
 */
fun interface ActionTagHandler {
    /**
     * @param player The player executing the action.
     * @param payload The raw string that comes AFTER the tag (e.g., for "[message] Hello", payload is "Hello").
     * @param handler The active GUI handler (nullable if executing before a GUI opens).
     */
    fun execute(response: ActionTagRegistryResponse)
}