package net.justlime.limeframegui.builder

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.enums.ChestGuiActions
import net.justlime.limeframegui.event.GuiClick
import net.justlime.limeframegui.event.GuiEventImpl
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.menu.GuiPage
import net.justlime.limeframegui.menu.GuiPageImpl
import net.justlime.limeframegui.menu.Navigation
import net.justlime.limeframegui.models.FrameReservedSlotPage
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.session.GuiSession
import net.justlime.limeframegui.util.toGuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

/**
 * The Builder is a Blueprint: ChestGuiBuilder is a configuration object.
 * Its job is to exist before any player interacts with the GUI.
 * Use it to define the layout, the pages, and the rules. It's like an architect's blueprint for a house.
 * - the setting contain empty stylesheet
 */
class ChestGUIBuilder(val session: GuiSession, originalSetting: GuiSetting) {

    private val setting: GuiSetting = originalSetting.clone()
    var buffer: GuiBuffer? = null

    /**Pages are temporarily stored here before being moved to the handler.*/
    val pages = mutableMapOf<Int, GuiPage>()

    /**Main Handler for Registering Events**/
    private val handler: GuiEventHandler = GuiEventImpl(session, setting)

    // All configuration steps are queued as prioritized actions to be executed in order during build().
    val actions = mutableListOf<Pair<ChestGuiActions, () -> Unit>>()
    private var currentExecutingAction: ChestGuiActions? = null

    val reservedSlot = FrameReservedSlotPage()

    // Store the navigation block to re-apply it to new pages
    var navigationBlock: (Navigation.() -> Unit)? = null

    init {
        // The global page (ID 0) is created immediately to hold shared items.
        pages[ChestGUI.GLOBAL_PAGE_ID] = createPage(ChestGUI.GLOBAL_PAGE_ID, setting)
    }

    // --- Global Event Handlers ---
    fun onOpen(handler: (InventoryOpenEvent) -> Unit) {
        actions.add(ChestGuiActions.GLOBAL_EVENT to {
            this.handler.globalOpenHandler = handler
        })
    }

    fun onClose(handler: (InventoryCloseEvent) -> Unit) {
        actions.add(ChestGuiActions.GLOBAL_EVENT to { this.handler.globalCloseHandler = handler })
    }

    fun onClick(handler: (InventoryClickEvent) -> Unit) {
        actions.add(ChestGuiActions.GLOBAL_EVENT to { this.handler.globalClickHandler = handler })
    }

    // Page Management
    /**
     * Adds a page with a specific, unique ID.
     * Throws an error if the ID is already in use or is the reserved global ID.
     */
    fun addPage(id: Int, setting: GuiSetting = this.setting, skipWaiting: Boolean = false, block: GuiPage.() -> Unit) {
        if (setting.style.isEmpty()) setting.style = this.setting.style
        val runBlock = {
            if (LimeFrameAPI.debugging) println("Starting Execution of Page $id")
            if (id == ChestGUI.GLOBAL_PAGE_ID) throw IllegalArgumentException("Cannot overwrite the global page (ID 0).")
            if (pages.containsKey(id)) throw IllegalArgumentException("A page with ID $id already exists.")
            val newPage = createPage(id, setting)
            pages[id] = newPage
            newPage.apply(block)

            if (LimeFrameAPI.debugging) println("Finished Execution of Page $id")
        }

        if (currentExecutingAction == ChestGuiActions.PAGE_ITEMS || skipWaiting) {
            runBlock()
        } else {
            actions += ChestGuiActions.PAGE_ITEMS to runBlock // Otherwise queue
            if (LimeFrameAPI.debugging) println("Queued Page $id")
        }
    }

    /**
     * Adds a page with an automatically assigned, incremental ID.
     */
    fun addPage(setting: GuiSetting = this.setting, skipWaiting: Boolean = false, block: GuiPage.() -> Unit) {
        if (setting.style.isEmpty()) setting.style = this.setting.style
        val runBlock = {
            val newId = (pages.keys.maxOrNull() ?: ChestGUI.GLOBAL_PAGE_ID) + 1
            if (LimeFrameAPI.debugging) println("Starting Execution of Page $newId")
            val newPage = createPage(newId, setting)
            pages[newId] = newPage
            newPage.apply(block) // <-- Note: "This will call addItem() and setItem() which is store on addPage{..}"
            if (LimeFrameAPI.debugging) println("Finished Execution of Page $newId")
        }

        if (currentExecutingAction == ChestGuiActions.PAGE_ITEMS || skipWaiting) {
            runBlock()
        } else {
            actions += ChestGuiActions.PAGE_ITEMS to runBlock
            if (LimeFrameAPI.debugging) println("Queued Page ${(pages.keys.maxOrNull() ?: ChestGUI.GLOBAL_PAGE_ID) + 1}")
        }
    }

    /**
     * Creates a new page, correctly copying all items and handlers from the global page.
     */
    private fun createPage(pageId: Int, setting: GuiSetting): GuiPage {
        val newPage = GuiPageImpl(this, handler, pageId, setting)

        val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] as? GuiPageImpl ?: return newPage

        //Copy Visuals (Inventory)
        globalPage.inventory.contents.forEachIndexed { slot, itemStack ->
            if (itemStack != null) {
                val isDynamic = globalPage.trackAddItemSlot.containsKey(slot)

                if (!isDynamic) {
                    newPage.inventory.setItem(slot, itemStack)
                }
            }
        }

        // Copy Cache (The Blueprint Data)
        globalPage.itemCache.forEach { (slot, guiItem) ->
            val isDynamic = globalPage.trackAddItemSlot.containsKey(slot)

            if (!isDynamic) {
                newPage.itemCache[slot] = guiItem
            }
        }

        // Copy Click Handlers
        handler.itemClickHandler[ChestGUI.GLOBAL_PAGE_ID]?.forEach { (slot, handler) ->
            val isDynamic = globalPage.trackAddItemSlot.containsKey(slot)

            if (!isDynamic) {
                val pageHandlers = this.handler.itemClickHandler.computeIfAbsent(pageId) { mutableMapOf() }
                pageHandlers[slot] = handler
            }
        }

        return newPage
    }

    // Item Management
    fun addItem(item: GuiItem?, onClick: GuiClick.(InventoryClickEvent) -> Unit = {}) {
        val runBlock = to@{
            if (item != null) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to
                globalPage.addItem(item, onClick)
            }
        }
        if (currentExecutingAction == ChestGuiActions.GLOBAL_ITEMS) runBlock()
        else actions += ChestGuiActions.GLOBAL_ITEMS to runBlock

    }

    fun addItem(items: List<GuiItem>, onClick: GuiClick.(GuiItem, InventoryClickEvent) -> Unit = { _, _ -> }) {
        val runBlock = to@{
            if (items.isEmpty()) return@to
            val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to

            items.forEach { guiItem ->
                globalPage.addItem(guiItem) { event -> onClick.invoke(this, guiItem, event) }
            }
        }
        if (currentExecutingAction == ChestGuiActions.GLOBAL_ITEMS) runBlock()
        else actions += ChestGuiActions.GLOBAL_ITEMS to runBlock
    }

    fun setItem(item: GuiItem?, onClick: GuiClick.(InventoryClickEvent) -> Unit = {}) {
        actions += ChestGuiActions.GLOBAL_ITEMS to {
            if (item != null) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to

                if (item.slot != null) {
                    globalPage.setItem(item.slot!!, item, false, onClick)
                }

                if (item.slotList.isNotEmpty()) {
                    item.slotList.forEach { slot ->
                        globalPage.setItem(slot, item, false, onClick)
                    }
                }
            }
        }
    }

    fun setItem(item: GuiItem?, slot: Int?, onClick: GuiClick.(InventoryClickEvent) -> Unit = {}) {
        actions += ChestGuiActions.GLOBAL_ITEMS to {
            if (slot != null && item != null) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to
                globalPage.setItem(slot, item, false, onClick)
            }
        }
    }

    fun setItem(items: GuiItem?, slot: List<Int>, onClick: GuiClick.(InventoryClickEvent) -> Unit = { _ -> }) {
        actions += ChestGuiActions.GLOBAL_ITEMS to {
            if (items != null && slot.isNotEmpty()) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to

                slot.forEach { currentSlot ->
                    globalPage.setItem(currentSlot, items) { event -> onClick.invoke(this, event) }
                }
            }
        }
    }

    fun nav(block: Navigation.() -> Unit = {}) {
        this.navigationBlock = block

        val navigation = Navigation(this, handler).apply(block)


        reservedSlot.enableNavSlotReservation = true
        reservedSlot.nextPageSlot = navigation.nextSlot
        reservedSlot.prevPageSlot = navigation.prevSlot
        reservedSlot.navMargin = navigation.margin



        if (LimeFrameAPI.debugging) println("Queued Navigation")
        val runBlock = { navigation.build() }

        if (currentExecutingAction == ChestGuiActions.NAVIGATION) runBlock()
        else actions += ChestGuiActions.NAVIGATION to runBlock
    }

    fun loadInventoryContents(inventory: Inventory) {
        for (i in 0 until inventory.size) {
            val itemStack = inventory.getItem(i) ?: continue
            val guiItem = itemStack.toGuiItem()
            setItem(guiItem, i)
        }
    }

    /**
     * Executes all queued actions in their prioritized order and returns the fully configured GuiImpl handler.
     */
    fun build(): GuiEventHandler {
        actions.sortedBy { it.first.priority }.forEach { (action, block) ->
            currentExecutingAction = action
            block()
            currentExecutingAction = null
        }

        pages.forEach { (id, page) ->
            handler.pageInventories[id] = page.inventory
        }

        return handler
    }

    /**
     * Refreshes the GUI for the current session.
     *
     * This method should be called when the underlying data of the GUI changes
     * and you want to update the displayed items for the player. It re-renders
     * all pages and re-applies navigation elements.
     *
     * Note: This method should not be called within `onOpen` or `addPage` blocks
     * as it can lead to infinite loops or unexpected behavior during GUI construction.
     */
    fun refresh() {
        session.refresh()
        val block = navigationBlock ?: return
        val navigation = Navigation(this, handler).apply(block)
        navigation.build()
    }

}