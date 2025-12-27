package net.justlime.limeframegui.type

import net.justlime.limeframegui.impl.ChestGUIBuilder
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.session.GuiSession
import org.bukkit.entity.Player

/**
 * The Blueprint class.
 * * This class DOES NOT hold any active inventories or listeners.
 * It simply holds the configuration (rows, title, builder block).
 * * When you call open(), it spins up a new [GuiSession].
 */
class ChestGUI(val setting: GuiSetting, val block: ChestGUIBuilder.() -> Unit = {}) {

    // Convenience constructor
    constructor(row: Int, title: String, block: ChestGUIBuilder.() -> Unit = {}) : this(GuiSetting(row, title), block)

    /**
     * Opens the GUI for a specific player.
     * * This creates a new independent Session, ensuring:
     * 1. Placeholders are parsed specifically for this player.
     * 2. No data leaks between players (Multiplayer Safe).
     */
    fun open(player: Player, page: Int? = null) {

        // Prepare the Style Context for this session
        val context = setting.style.copy().apply {
            viewer = player
            if (offlinePlayer == null) offlinePlayer = player

        }

        // Start the Session for player).
        val session = GuiSession(this, context)
        session.start(page)
    }

    fun openAsync(player: Player, page: Int? = null) {
        //TODO

    }


    companion object {
        const val GLOBAL_PAGE_ID = 0
    }
}