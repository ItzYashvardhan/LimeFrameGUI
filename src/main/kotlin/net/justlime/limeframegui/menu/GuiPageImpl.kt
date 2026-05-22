package net.justlime.limeframegui.menu

import net.justlime.limeframegui.builder.ChestGUIBuilder
import net.justlime.limeframegui.engine.ConditionEngine
import net.justlime.limeframegui.event.GuiClick
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.util.item
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

class GuiPageImpl(
    val builder: ChestGUIBuilder,
    override val handler: GuiEventHandler,
    override val viewerPage: Int,
    private val setting: GuiSetting
) : GuiPage {

    private var trackGuiPage: GuiPage = this

    override var inventory = handler.createPageInventory(viewerPage, setting)

    // 🌟 CHANGED: Now a MutableList to support conditional stacking!
    override val itemCache = mutableListOf<GuiItem>()

    override val style: GuiStyleSheet = builder.session.context
    override var isRendered: Boolean = false

    // 🌟 CHANGED: Return the List directly
    override fun getItems(): List<GuiItem> = itemCache

    override var trackAddItemSlot = mutableMapOf<Int, Pair<GuiItem, GuiClick.(InventoryClickEvent) -> Unit>>()

    override fun addPage(id: Int, setting: GuiSetting, block: GuiPage.() -> Unit) =
        builder.addPage(id, setting, true, block)

    override fun addPage(setting: GuiSetting, block: GuiPage.() -> Unit) = builder.addPage(setting, true, block)

    override fun addItem(item: GuiItem, onClick: GuiClick.(InventoryClickEvent) -> Unit): Int {
        val newItem = item.clone()

        if (builder.pages.size == 1 || !builder.reservedSlot.enableNavSlotReservation) {
            val freeSlot = findFreeSlot(itemCache)
            if (freeSlot != -1) {
                newItem.slot = freeSlot
                itemCache.add(newItem)
                registerClickEvent(newItem, freeSlot, onClick)
            }
            return freeSlot
        }

        val currentPage = trackGuiPage
        val currentImpl = currentPage as? GuiPageImpl ?: return -1
        val nextFreeSlot = currentImpl.findFreeSlot(currentImpl.itemCache)

        if (nextFreeSlot != -1) {
            newItem.slot = nextFreeSlot
            currentImpl.itemCache.add(newItem)
            currentImpl.trackAddItemSlot[nextFreeSlot] = newItem to onClick
            currentImpl.registerClickEvent(newItem, nextFreeSlot, onClick)
            return nextFreeSlot
        }

        var resultSlot = -1
        builder.addPage(setting, true) {
            trackGuiPage = this
            resultSlot = this.addItem(newItem, onClick)
        }
        return resultSlot
    }

    override fun addItem(items: List<GuiItem>, onClick: GuiClick.(GuiItem, InventoryClickEvent) -> Unit) {
        items.forEach { guiItem ->
            addItem(guiItem) { event -> onClick.invoke(this, guiItem, event) }
        }
    }

    override fun addItemLater(item: GuiItem, onClick: GuiClick.(InventoryClickEvent) -> Unit): Int {
        val itemSlot = addItem(item, onClick)
        builder.refresh()
        return itemSlot
    }

    override fun setItem(
        index: Int,
        item: GuiItem,
        dynamic: Boolean,
        onClick: GuiClick.(InventoryClickEvent) -> Unit
    ): Int {
        val newItem = item.clone().apply { slot = index }
        if (index < inventory.size) {
            itemCache.add(newItem)
            registerClickEvent(newItem, index, onClick)

            // Programmatic dynamic updates bypass the condition engine for legacy support
            if (dynamic) inventory.setItem(index, newItem.toItemStack())
            return index
        }
        return -1
    }

    override fun remove(slot: Int): GuiPage {
        if (builder.pages[viewerPage]?.trackAddItemSlot?.containsKey(slot) != true) {
            inventory.setItem(slot, null)
            // 🌟 CHANGED: Remove all items sharing this slot
            itemCache.removeAll { it.slot == slot }
            handler.itemClickHandler[viewerPage]?.remove(slot)
            return this
        }

        val dynamicItems = mutableListOf<Triple<Int, Int, Pair<GuiItem, GuiClick.(InventoryClickEvent) -> Unit>>>()
        builder.pages.toSortedMap().forEach { (pageId, guiPage) ->
            guiPage.trackAddItemSlot.toSortedMap().forEach { (itemSlot, itemData) ->
                dynamicItems.add(Triple(pageId, itemSlot, itemData))
            }
        }

        val removalIndex = dynamicItems.indexOfFirst { (pageId, itemSlot, _) ->
            pageId == viewerPage && itemSlot == slot
        }

        if (removalIndex == -1) return this

        for (i in removalIndex until dynamicItems.size - 1) {
            val targetLocation = dynamicItems[i]
            val sourceItem = dynamicItems[i + 1]

            val targetPageId = targetLocation.first
            val targetSlot = targetLocation.second
            val (sourceItemData, sourceClickHandler) = sourceItem.third

            val targetPage = builder.pages[targetPageId] ?: continue

            targetPage.setItem(targetSlot, sourceItemData, false, sourceClickHandler)
            targetPage.trackAddItemSlot[targetSlot] = sourceItem.third
        }

        dynamicItems.lastOrNull()?.let { lastItemLocation ->
            val (lastItemPageId, lastItemSlot) = lastItemLocation
            val lastItemPage = builder.pages[lastItemPageId]

            lastItemPage?.inventory?.setItem(lastItemSlot, null)

            // 🌟 CHANGED: Remove all items on this slot
            (lastItemPage as? GuiPageImpl)?.itemCache?.removeAll { it.slot == lastItemSlot }

            handler.itemClickHandler[lastItemPageId]?.remove(lastItemSlot)
            lastItemPage?.trackAddItemSlot?.remove(lastItemSlot)
        }
        return this
    }

    override fun remove(slotList: List<Int>): GuiPage {
        slotList.sortedDescending().forEach { remove(it) }
        return this
    }

    override fun nav(block: Navigation.() -> Unit) {
        throw IllegalStateException("Navigation can only be configured at the top-level GUI builder.")
    }

    override fun onClick(event: (InventoryClickEvent) -> Unit) {
        handler.pageClickHandlers[viewerPage] = event
    }

    override fun onOpen(block: (InventoryOpenEvent) -> Unit) {
        handler.pageOpenHandlers[viewerPage] = block
    }

    override fun openPage(player: Player, id: Int) {
        handler.open(player, id)
    }

    // 🌟 CHANGED: Maps occupied slots from the List to find a free space
    private fun findFreeSlot(contents: List<GuiItem>): Int {
        val reserved = getReservedSlots(inventory)
        val occupiedSlots = contents.mapNotNull { it.slot }.toSet()

        for (i in 0 until inventory.size) {
            if (i in reserved) continue
            if (i !in occupiedSlots) {
                return i
            }
        }
        return -1
    }

    private fun getReservedSlots(inventory: Inventory): Set<Int> {
        val lastSlot = inventory.size - 1
        val lastRowFirstSlot = lastSlot - 8
        val margin = builder.reservedSlot.navMargin

        return buildSet {
            addAll(builder.reservedSlot.otherSlot)

            if (builder.reservedSlot.enableNavSlotReservation) {
                if (builder.reservedSlot.nextPageSlot != -1) {
                    add(builder.reservedSlot.nextPageSlot)
                } else {
                    add(lastSlot - margin)
                    addAll((lastSlot - margin + 1)..lastSlot)
                }

                if (builder.reservedSlot.prevPageSlot != -1) {
                    add(builder.reservedSlot.prevPageSlot)
                } else {
                    add(lastRowFirstSlot + margin)
                    addAll(lastRowFirstSlot until (lastRowFirstSlot + margin))
                }
            }
        }
    }

    /**
     * CLICK ROUTER
     * Evaluates which item is actually visible to the player and runs THAT specific item's code.
     */
    private fun registerClickEvent(item: GuiItem, slot: Int, onClick: GuiClick.(InventoryClickEvent) -> Unit) {

        item.onClick = { event ->
            val guiClick = GuiClick(event)
            onClick(guiClick, event)
        }

        handler.itemClickHandler.computeIfAbsent(viewerPage) { mutableMapOf() }[slot] = { event ->
            val player = event.whoClicked as? Player
            if (player != null) {
                val activeItem = itemCache.filter { it.slot == slot }
                    .sortedByDescending { it.priority }
                    .firstOrNull { ConditionEngine.checkRequirements(player, it.viewRequirements) }
                if (activeItem != null) {
                    event.item = activeItem
                    activeItem.onClick.invoke(event)
                }
            }
        }
    }
}