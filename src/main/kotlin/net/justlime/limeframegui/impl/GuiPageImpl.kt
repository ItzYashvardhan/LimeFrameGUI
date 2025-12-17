package net.justlime.limeframegui.impl

import net.justlime.limeframegui.handler.GUIEventHandler
import net.justlime.limeframegui.handler.GuiPage
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.utilities.item
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

class GuiPageImpl(val builder: ChestGUIBuilder, override val handler: GUIEventHandler, override val currentPage: Int, private val setting: GUISetting) : GuiPage {

    private var trackGuiPage: GuiPage = this

    override var inventory = handler.createPageInventory(currentPage, setting)

    override val itemCache = mutableMapOf<Int, GuiItem>()

    override fun getItems(): Map<Int, GuiItem> = itemCache

    override var trackAddItemSlot = mutableMapOf<Int, Pair<GuiItem, (InventoryClickEvent) -> Unit>>()

    // For Nested Page Only
    override fun addPage(id: Int, setting: GUISetting, block: GuiPage.() -> Unit) = builder.addPage(id, setting, block)

    // For Nested Page Only
    override fun addPage(setting: GUISetting, block: GuiPage.() -> Unit) = builder.addPage(setting, block)

    override fun addItem(item: GuiItem, onClick: (InventoryClickEvent) -> Unit): Int {
        val newItem = item.clone()
        newItem.applyStyleSheet()

        //Handle for single page (Global Page) or nav is disabled
        if (builder.pages.size == 1 || !builder.reservedSlot.enableNavSlotReservation) {
            val freeSlot = findFreeSlot(itemCache)
            if (freeSlot != -1) {
                itemCache[freeSlot] = newItem
                registerClickEvent(newItem, freeSlot, onClick)
            }
            return freeSlot
        }

        //Handle for Pagination
        val currentPage = trackGuiPage
        val currentImpl = currentPage as? GuiPageImpl ?: return -1
        val nextFreeSlot = currentImpl.findFreeSlot(currentImpl.itemCache)

        if (nextFreeSlot != -1) {
            currentImpl.itemCache[nextFreeSlot] = newItem
            currentImpl.trackAddItemSlot[nextFreeSlot] = newItem to onClick
            currentImpl.registerClickEvent(newItem, nextFreeSlot, onClick)
            return nextFreeSlot
        }

        //Create New Page (If current is full)
        var resultSlot = -1
        builder.addPage(setting) {
            trackGuiPage = this
            resultSlot = this.addItem(newItem, onClick)
        }
        return resultSlot
    }

    override fun addItem(items: List<GuiItem>, onClick: ((GuiItem, InventoryClickEvent) -> Unit)) {
        items.forEach { guiItem ->
            addItem(guiItem) { event -> onClick.invoke(guiItem, event) }
        }
    }

    override fun setItem(index: Int, item: GuiItem, onClick: ((InventoryClickEvent) -> Unit)): Int {
        val newItem = item.clone()
        if (index < inventory.size) {
            newItem.applyStyleSheet()
            itemCache[index] = newItem
            registerClickEvent(newItem, index, onClick)
            return index
        }
        return -1
    }

    override fun remove(slot: Int): GuiPage {

        // Ensure the item being removed is a dynamically added one on the current page.
        if (builder.pages[currentPage]?.trackAddItemSlot?.containsKey(slot) != true) {
            // If not, just clear the slot and do nothing else.
            inventory.setItem(slot, null)

            itemCache.remove(slot)

            handler.itemClickHandler[currentPage]?.remove(slot)
            return this
        }

        // 1. Collect all dynamically added items from all pages into a single, ordered list.
        // This list will represent the continuous space that items occupy.
        val dynamicItems = mutableListOf<Triple<Int, Int, Pair<GuiItem, (InventoryClickEvent) -> Unit>>>()
        builder.pages.toSortedMap().forEach { (pageId, guiPage) ->
            // Sort by slot to ensure items on the same page are in order.
            guiPage.trackAddItemSlot.toSortedMap().forEach { (itemSlot, itemData) ->
                dynamicItems.add(Triple(pageId, itemSlot, itemData))
            }
        }

        // 2. Find the linear index of the item we need to remove.
        val removalIndex = dynamicItems.indexOfFirst { (pageId, itemSlot, _) ->
            pageId == currentPage && itemSlot == slot
        }

        // This should always be found due to the initial check, but as a safeguard:
        if (removalIndex == -1) return this

        // 3. Shift all subsequent items forward by one position.
        // We iterate from the removal index to the second-to-last item.
        for (i in removalIndex until dynamicItems.size - 1) {
            val targetLocation = dynamicItems[i]
            val sourceItem = dynamicItems[i + 1]

            val targetPageId = targetLocation.first
            val targetSlot = targetLocation.second
            val (sourceItemData, sourceClickHandler) = sourceItem.third

            val targetPage = builder.pages[targetPageId] ?: continue

            // Move the source item to the target slot.
            targetPage.setItem(targetSlot, sourceItemData, sourceClickHandler)
            targetPage.trackAddItemSlot[targetSlot] = sourceItem.third
        }

        // 4. Clear the last item's original slot, as it has now been moved.
        dynamicItems.lastOrNull()?.let { lastItemLocation ->
            val (lastItemPageId, lastItemSlot) = lastItemLocation
            val lastItemPage = builder.pages[lastItemPageId]

            lastItemPage?.inventory?.setItem(lastItemSlot, null)

            (lastItemPage as? GuiPageImpl)?.itemCache?.remove(lastItemSlot)

            handler.itemClickHandler[lastItemPageId]?.remove(lastItemSlot)
            lastItemPage?.trackAddItemSlot?.remove(lastItemSlot)
        }
        return this
    }

    override fun remove(slotList: List<Int>): GuiPage {
        // Sort descending to avoid index shifting issues when removing multiple items.
        slotList.sortedDescending().forEach { remove(it) }
        return this
    }

    override fun nav(block: Navigation.() -> Unit) {
        throw IllegalStateException("Navigation can only be configured at the top-level GUI builder. Its not ideal to be used in nested pages")
    }

    override fun onOpen(handler: (InventoryOpenEvent) -> Unit) {
        this.handler.pageOpenHandlers[currentPage] = handler

    }

    override fun onClose(handler: (InventoryCloseEvent) -> Unit) {
        this.handler.pageCloseHandlers[currentPage] = handler
    }

    override fun onClick(handler: (InventoryClickEvent) -> Unit) {
        this.handler.pageClickHandlers[currentPage] = handler
    }

    override fun openPage(player: Player, id: Int) {
        handler.open(player, id)
    }

    private fun GuiItem.applyStyleSheet() {
        this.style.let {
            if (it.player == null) it.player = setting.style.player
            if (it.offlinePlayer == null) it.offlinePlayer = setting.style.offlinePlayer
            if (it.placeholder.isEmpty()) it.placeholder = setting.style.placeholder
            if (it.openSound.isEmpty()) it.openSound = setting.style.openSound
            if (it.closeSound.isEmpty()) it.closeSound = setting.style.closeSound
            if (it.clickSound.isEmpty()) it.clickSound = setting.style.clickSound
        }
    }

    private fun findFreeSlot(contents: Map<Int, GuiItem>): Int {
        val reserved = getReservedSlots(inventory)

        for (i in 0 until inventory.size) {

            if (i in reserved) continue

            if (!contents.containsKey(i)) {
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
                // Next Page Area
                if (builder.reservedSlot.nextPageSlot != -1) {
                    add(builder.reservedSlot.nextPageSlot)
                } else {
                    add(lastSlot - margin)
                    addAll((lastSlot - margin + 1)..lastSlot)
                }

                // Previous Page Area
                if (builder.reservedSlot.prevPageSlot != -1) {
                    add(builder.reservedSlot.prevPageSlot)
                } else {
                    add(lastRowFirstSlot + margin)
                    // Reserve the rest of the bottom-left row (corner)
                    addAll(lastRowFirstSlot until (lastRowFirstSlot + margin))
                }
            }
        }
    }

    private fun registerClickEvent(item: GuiItem, slot: Int, onClick: (InventoryClickEvent) -> Unit) {
        handler.itemClickHandler.computeIfAbsent(currentPage) { mutableMapOf() }[slot] = { event ->
            event.item = item
            item.onClick(event)
            onClick(event)
        }
    }
}