package net.justlime.limeframegui.handler

import net.justlime.limeframegui.models.GUISetting
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.java.JavaPlugin

/**
 *  * Handles all GUI-related events and manages the state of open GUIs.
 * This interface defines the contract for how GUI events are processed
 * and how GUI pages are managed.
 **/
interface GUIEventHandler : InventoryHolder {

    /** Global event handlers that apply to all pages.**/
    var globalOpenHandler: ((InventoryOpenEvent) -> Unit)?
    var globalCloseHandler: ((InventoryCloseEvent) -> Unit)?
    var globalClickHandler: ((InventoryClickEvent) -> Unit)?

    /** Page-specific handlers, mapping a page ID to a single handler function.**/
    val pageOpenHandlers: MutableMap<Int, (InventoryOpenEvent) -> Unit>
    val pageCloseHandlers: MutableMap<Int, (InventoryCloseEvent) -> Unit>
    val pageClickHandlers: MutableMap<Int, (InventoryClickEvent) -> Unit>

    /**
     * A single, unified map for all item-specific click handlers.
     * - Structure: Page ID -> (Slot -> Handler)
     **/
    val itemClickHandler: MutableMap<Int, MutableMap<Int, (InventoryClickEvent) -> Unit>>

    /**
     * Stores the actual Inventory object for each page. This is populated by the builder
     * - Structure: Page ID -> Inventory
     * **/
    val pageInventories: MutableMap<Int, Inventory>


    /**
     * The inventory associated with this GUI
     * - Structure: Inventory
     * **/
    override fun getInventory(): Inventory

    /**
     * Tracks the current page for each player viewing the GUI.
     * - Structure: Player Name -> Page ID
     * **/
    val currentPages: MutableMap<String, Int>

    fun setCurrentPage(player: Player, page: Int)
    fun getCurrentPage(player: Player): Int?

    /** Creates an empty page inventory*/
    fun createPageInventory(id: Int, setting: GUISetting): Inventory


    /** Register inventory open events to Inventory Listener.**/
    fun onEvent(event: InventoryOpenEvent)

    /** Register inventory click events to Inventory Listener.**/
    fun onEvent(event: InventoryClickEvent)

    /** Register inventory close events to Inventory Listener.**/
    fun onEvent(event: InventoryCloseEvent, plugin: JavaPlugin)

    /**
     * Opens a specific page of the GUI for a player.
     *
     * @param player The player to open the GUI for.
     * @param page The ID of the page to open. Defaults to 0.
     *
     * @return true if page found else false
     */
    fun open(player: Player, page: Int = 0): Boolean
}