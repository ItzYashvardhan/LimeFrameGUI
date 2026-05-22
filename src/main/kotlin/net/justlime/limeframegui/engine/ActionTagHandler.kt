package net.justlime.limeframegui.engine

import net.justlime.limeframegui.event.GuiEventHandler
import org.bukkit.entity.Player

/**
 * The functional interface for handling action tags.
 */
fun interface ActionTagHandler {
    /**
     * @param player The player executing the action.
     * @param payload The raw string that comes AFTER the tag (e.g., for "[message] Hello", payload is "Hello").
     * @param gui The active GUI handler (nullable if executing before a GUI opens).
     */
    fun execute(player: Player, payload: String, gui: GuiEventHandler?)
}