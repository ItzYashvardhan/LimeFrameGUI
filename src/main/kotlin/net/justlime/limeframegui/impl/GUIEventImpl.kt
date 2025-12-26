package net.justlime.limeframegui.impl

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.handler.GUIEventHandler
import net.justlime.limeframegui.integration.FoliaLibHook
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.type.ChestGUI
import net.justlime.limeframegui.utilities.FrameAdapter
import net.justlime.limeframegui.utilities.item
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin

/**
 * The core implementation of the GUI handler.
 *
 * It simplifies event handler management, improves event processing logic,
 * and adds the ability to open specific pages.
 *
 * @param setting The basic settings for the GUI (title, rows).
 */
class GUIEventImpl(private val setting: GUISetting) : GUIEventHandler {
    private val hasTriggeredGlobalOpen = mutableSetOf<String>()

    /** A single, optional handler for global events. **/
    override var globalOpenHandler: ((InventoryOpenEvent) -> Unit)? = null
    override var globalCloseHandler: ((InventoryCloseEvent) -> Unit)? = null
    override var globalClickHandler: ((InventoryClickEvent) -> Unit)? = null

    /** Page-specific handlers, mapping a page ID to a single handler function**/
    override val pageOpenHandlers = mutableMapOf<Int, (InventoryOpenEvent) -> Unit>()
    override val pageCloseHandlers = mutableMapOf<Int, (InventoryCloseEvent) -> Unit>()
    override val pageClickHandlers = mutableMapOf<Int, (InventoryClickEvent) -> Unit>()

    /** A single, unified map for all item-specific click handlers.**/
    // Structure: Page ID -> (Slot -> Handler)
    override val itemClickHandler = mutableMapOf<Int, MutableMap<Int, (InventoryClickEvent) -> Unit>>()

    // Stores the actual Inventory object for each page. This is populated by the builder.
    override val pageInventories = mutableMapOf<Int, Inventory>()

    // Tracks the current page for each player viewing the GUI.
    override val currentPages = mutableMapOf<String, Int>()

    /**
     * Opens a specific page of the GUI for a player.
     *
     * @param player The player to open the GUI for.
     * @param page The ID of the page to open. Defaults to minimum page id.
     */
    override fun open(player: Player, page: Int): Boolean {

        val inventoryToOpen = pageInventories[page] ?: return false

        setCurrentPage(player, page)

        // Open the inventory for the player.
        player.openInventory(inventoryToOpen)
        return true
    }

    /**
     * The base Inventory of Page. It means its content will copy to all pages.
     */
    override fun getInventory(): Inventory {
        return pageInventories[ChestGUI.GLOBAL_PAGE_ID] ?: createPageInventory(ChestGUI.GLOBAL_PAGE_ID, setting)
    }

    override fun createPageInventory(id: Int, setting: GUISetting): Inventory {
        val size = setting.rows * 9
        val title = setting.title
        val inv = Bukkit.createInventory(this, size, title)
        pageInventories[id] = inv
        return inv
    }

    /**
     * Sets the current page for a player. This is typically called by the builder or the open function.
     */
    override fun setCurrentPage(player: Player, page: Int) {
        currentPages[player.name] = page
    }

    /**
     * Gets the current page a player is viewing. Defaults to 0.
     */
    override fun getCurrentPage(player: Player): Int? {
        return currentPages[player.name]
    }

    /**
     * Handles inventory open events.
     */
    override fun onEvent(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val pageId = getCurrentPage(player)

        pageOpenHandlers[pageId]?.invoke(event)

        if (!hasTriggeredGlobalOpen.contains(player.name)) {
            globalOpenHandler?.invoke(event)
            hasTriggeredGlobalOpen.add(player.name)
            setting.style.openSound.playSound(player)
        }
    }

    /**
     * Handles click events with a clear priority system, stopping if the event is canceled.
     */
    override fun onEvent(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val slot = event.slot
        val pageId = getCurrentPage(player) ?: return

        // Priority 1: Page-and-slot-specific handler.
        itemClickHandler[pageId]?.get(slot)?.invoke(event)

        // Priority 2: Page-wide click handler.
        pageClickHandlers[pageId]?.invoke(event)

        // Priority 3: Global click handler.
        globalClickHandler?.invoke(event)

        val soundToPlay = event.item?.style?.clickSound?.takeIf { !it.isEmpty() } ?: setting.style.clickSound
        soundToPlay.playSound(player)

    }

    /**
     * Handles inventory close events and cleans up player tracking to prevent memory leaks.
     */
    override fun onEvent(event: InventoryCloseEvent, plugin: JavaPlugin) {
        val player = event.player as? Player ?: return
        val pageId = getCurrentPage(player)

        // Fire handlers first.
        pageCloseHandlers[pageId]?.invoke(event)

        // Clean up the player's page tracking to prevent memory leaks.
        if (FoliaLibHook.isInitialized()) {
            FoliaLibHook.foliaLib.scheduler.runNextTick {
                val openInventory = FrameAdapter.getTopInventorySafe(player)
                if (!pageInventories.containsValue(openInventory)) {
                    globalCloseHandler?.invoke(event)
                    currentPages.remove(player.name)
                    hasTriggeredGlobalOpen.remove(player.name)
                    setting.style.closeSound.playSound(player)
                }
            }
            return
        }


        Bukkit.getScheduler().runTask(plugin, Runnable {
            val openInventory = FrameAdapter.getTopInventorySafe(player)
            if (!pageInventories.containsValue(openInventory)) {
                globalCloseHandler?.invoke(event)
                currentPages.remove(player.name)
                hasTriggeredGlobalOpen.remove(player.name)
            }
        })

    }

}