package net.justlime.limeframegui.type

import net.justlime.limeframegui.handle.GUIEventHandler
import net.justlime.limeframegui.impl.ChestGUIBuilder
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.models.LimeStyleSheet
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Initializes a new ChestGUI instance using a builder pattern.
 *
 * @param newSetting The settings for the GUI, including rows, title, and optional player for placeholders.
 */
class ChestGUI(val setting: GUISetting, private val block: ChestGUIBuilder.() -> Unit = {}) {
    constructor(row: Int, title: String, block: ChestGUIBuilder.() -> Unit = {}) : this(GUISetting(row, title), block)
    val newSetting = this.setting.clone()

    

    private lateinit var guiHandler: GUIEventHandler
    private lateinit var pages: MutableMap<Int, Inventory>
    var minPageId: Int = GLOBAL_PAGE_ID

    fun build() {
        if (this::guiHandler.isInitialized) return
        val builder = ChestGUIBuilder(newSetting)
        builder.apply(block)
        this.guiHandler = builder.build()
        pages = guiHandler.pageInventories
        minPageId = pages.keys.filter { it != GLOBAL_PAGE_ID }.minOrNull() ?: 0
    }

    /**
     * Opens the GUI for a specific player.
     * @param player The player to open the GUI for.
     * @param page The page number to open to.
     */
    fun open(player: Player, page: Int? = null) {
        if (newSetting.styleSheet == null) newSetting.styleSheet = LimeStyleSheet(player)
        if (newSetting.styleSheet?.player == null && newSetting.styleSheet?.offlinePlayer == null) {
            newSetting.styleSheet?.player = player
            build()
            guiHandler.open(player, page ?: minPageId)
            return
        }
        build()
        guiHandler.open(player, page ?: minPageId)
    }

    companion object {
        const val GLOBAL_PAGE_ID = 0
    }
}