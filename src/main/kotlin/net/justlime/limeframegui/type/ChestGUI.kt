package net.justlime.limeframegui.type

import net.justlime.limeframegui.handle.GUIEventHandler
import net.justlime.limeframegui.impl.ChestGUIBuilder
import net.justlime.limeframegui.models.GUISetting
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Initializes a new ChestGUI instance using a builder pattern.
 *
 * @param setting The settings for the GUI, including rows, title, and optional player for placeholders.
 */
class ChestGUI(val setting: GUISetting, private val block: ChestGUIBuilder.() -> Unit = {}) {
    constructor(row: Int, title: String, player: Player? = null, block: ChestGUIBuilder.() -> Unit) : this(GUISetting(row, title, player), block)

    private lateinit var guiHandler: GUIEventHandler
    private lateinit var pages: MutableMap<Int, Inventory>
    var minPageId: Int = 0
    var maxPageId: Int = 0


    fun init() {
        if (this::guiHandler.isInitialized) return
        val builder = ChestGUIBuilder(setting)
        builder.apply(block)
        this.guiHandler = builder.build()
        pages = guiHandler.pageInventories
        minPageId = pages.keys.filter { it != GLOBAL_PAGE }.minOrNull() ?: 0
        maxPageId = pages.keys.maxOrNull() ?: 0
    }

    /**
     * Opens the GUI for a given placeholder player.
     * @param page The page number to open to.
     */
    fun open(page: Int = minPageId) {
        init()
        setting.placeholderPlayer?.let {
            guiHandler.open(it, page)
        }
    }

    /**
     * Opens the GUI for a specific player.
     * @param player The player to open the GUI for.
     * @param page The page number to open to.
     */
    fun open(player: Player, page: Int? = null) {
        if (setting.placeholderPlayer == null && setting.placeholderOfflinePlayer == null) {
            setting.placeholderPlayer = player
            init()
            guiHandler.open(player, page?: minPageId)
            return
        }
        init()
        guiHandler.open(player, page?: minPageId)
    }

    companion object {
        const val GLOBAL_PAGE = 0
    }
}