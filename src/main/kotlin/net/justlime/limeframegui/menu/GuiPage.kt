package net.justlime.limeframegui.menu

import net.justlime.limeframegui.event.GuiClick
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.models.GuiStyleSheet
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

interface GuiPage {
    val viewerPage: Int
    var inventory: Inventory
    val itemCache: MutableList<GuiItem>
    val style: GuiStyleSheet
    var isRendered: Boolean // Flag to check if the page has been rendered with content

    /**Structure: Slot -> Item to Click**/
    var trackAddItemSlot: MutableMap<Int, Pair<GuiItem, GuiClick.(InventoryClickEvent) -> Unit>>
    val handler: GuiEventHandler

    fun getItems(): List<GuiItem>
    fun addItem(item: GuiItem, onClick: GuiClick.(InventoryClickEvent) -> Unit = {}): Int
    fun addItemLater(item: GuiItem, onClick: GuiClick.(InventoryClickEvent) -> Unit = {}): Int
    fun addItem(items: List<GuiItem>, onClick: GuiClick.(GuiItem, InventoryClickEvent) -> Unit = { _, _ -> })
    fun setItem(
        index: Int,
        item: GuiItem,
        dynamic: Boolean = false,
        onClick: GuiClick.(InventoryClickEvent) -> Unit = {}
    ): Int

    fun remove(slot: Int): GuiPage
    fun remove(slotList: List<Int>): GuiPage
    fun onClick(event: (InventoryClickEvent) -> Unit)
    fun onOpen(block: (InventoryOpenEvent) -> Unit)
    fun addPage(id: Int, setting: GuiSetting, block: GuiPage.() -> Unit)
    fun addPage(setting: GuiSetting, block: GuiPage.() -> Unit)
    fun nav(block: Navigation.() -> Unit)
    fun openPage(player: Player, id: Int)

}