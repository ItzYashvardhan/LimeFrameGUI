package net.justlime.limeframegui.handler

import net.justlime.limeframegui.impl.Navigation
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.models.GuiItem
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

interface GuiPage {
    val currentPage: Int
    var inventory: Inventory
    val itemCache: MutableMap<Int, GuiItem>

    /**Structure: Slot -> Item to Click**/
    var trackAddItemSlot: MutableMap<Int, Pair<GuiItem, (InventoryClickEvent) -> Unit>>
    val handler: GUIEventHandler

    fun getItems(): Map<Int, GuiItem>
    fun addItem(item: GuiItem, onClick: ((InventoryClickEvent) -> Unit) = {}): Int
    fun addItem(items: List<GuiItem>, onClick: ((GuiItem, InventoryClickEvent) -> Unit) = { _, _ -> })
    fun setItem(index: Int, item: GuiItem, onClick: ((InventoryClickEvent) -> Unit) = {}): Int
    fun remove(slot: Int): GuiPage
    fun remove(slotList: List<Int>): GuiPage
    fun onOpen(handler: (InventoryOpenEvent) -> Unit)
    fun onClose(handler: (InventoryCloseEvent) -> Unit)
    fun onClick(handler: (InventoryClickEvent) -> Unit)
    fun addPage(id: Int, setting: GUISetting, block: GuiPage.() -> Unit)
    fun addPage(setting: GUISetting, block: GuiPage.() -> Unit)
    fun nav(block: Navigation.() -> Unit)
    fun openPage(player: Player, id: Int)

}
