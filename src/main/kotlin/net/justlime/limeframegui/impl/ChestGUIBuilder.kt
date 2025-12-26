package net.justlime.limeframegui.impl

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.enums.ChestGuiActions
import net.justlime.limeframegui.handler.GUIEventHandler
import net.justlime.limeframegui.handler.GuiPage
import net.justlime.limeframegui.models.FrameReservedSlotPage
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.session.GuiSession
import net.justlime.limeframegui.type.ChestGUI
import net.justlime.limeframegui.utilities.toGuiItem
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
class ChestGUIBuilder(val session: GuiSession, originalSetting: GUISetting) {

    val setting: GUISetting = originalSetting.clone()
    var buffer : GuiBuffer? = null


    /**
     * The lazy-loading store. All items that don't fit on manually-defined
     * pages will be stored here, waiting to be rendered.
     */
    private val paginationStore = mutableListOf<Pair<GuiItem, (InventoryClickEvent) -> Unit>>() //TODO

    /**
     * Tracks the highest ID of a page you defined manually (e.g., in an `addPage(0) {}` block).
     * This tells us where the lazy-loaded pages should begin.
     */
    private var lastManualPageId = 0 //TODO

    /**
     * The total number of pages, combining manual and paginated ones.
     * This will be calculated once, after the setup block.
     */
    private var totalPageCount = 0 //TODO

    /**Pages are temporarily stored here before being moved to the handler.*/
    val pages = mutableMapOf<Int, GuiPage>()

    /**Main Handler for Registering Events**/
    private val guiHandler: GUIEventHandler = GUIEventImpl(setting)

    // All configuration steps are queued as prioritized actions to be executed in order during build().
    private val actions = mutableListOf<Pair<ChestGuiActions, () -> Unit>>()
    private var currentExecutingAction: ChestGuiActions? = null

    val reservedSlot = FrameReservedSlotPage()

    init {
        // The global page (ID 0) is created immediately to hold shared items.
        pages[ChestGUI.GLOBAL_PAGE_ID] = createPage(ChestGUI.GLOBAL_PAGE_ID, setting)
    }

    // --- Global Event Handlers ---

    fun onOpen(handler: (InventoryOpenEvent) -> Unit) {
        actions.add(ChestGuiActions.GLOBAL_EVENT to {
            guiHandler.globalOpenHandler = handler
        })
    }

    fun onPageOpen(handler: (InventoryOpenEvent) -> Unit) {
        actions.add(ChestGuiActions.PAGE_EVENT to {
            // Iterate over the pages defined in the builder, not the handler's (likely empty) map.
            pages.keys.forEach { pageId ->
                guiHandler.pageOpenHandlers[pageId] = handler
            }
        })
    }

    fun onClose(handler: (InventoryCloseEvent) -> Unit) {
        actions.add(ChestGuiActions.GLOBAL_EVENT to { guiHandler.globalCloseHandler = handler })
    }

    fun onPageClose(handler: (InventoryCloseEvent) -> Unit) {
        actions.add(ChestGuiActions.PAGE_EVENT to {
            pages.keys.forEach { pageId ->
                guiHandler.pageCloseHandlers[pageId] = handler
            }
        })
    }

    fun onClick(handler: (InventoryClickEvent) -> Unit) {
        actions.add(ChestGuiActions.GLOBAL_EVENT to { guiHandler.globalClickHandler = handler })
    }

    // Page Management
    /**
     * Adds a page with a specific, unique ID.
     * Throws an error if the ID is already in use or is the reserved global ID.
     */
    fun addPage(id: Int, setting: GUISetting = this.setting, block: GuiPage.() -> Unit) {
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

        if (currentExecutingAction == ChestGuiActions.PAGE_ITEMS) {
            runBlock() // We are inside PAGE_ITEMS, run immediately
        } else {
            actions += ChestGuiActions.PAGE_ITEMS to runBlock // Otherwise queue
            if (LimeFrameAPI.debugging) println("Queued Page $id")
        }
    }

    /**
     * Adds a page with an automatically assigned, incremental ID. This is the recommended approach.
     */
    fun addPage(setting: GUISetting = this.setting, block: GuiPage.() -> Unit) {
        if (setting.style.isEmpty()) setting.style = this.setting.style
        val runBlock = {
            val newId = (pages.keys.maxOrNull() ?: ChestGUI.GLOBAL_PAGE_ID) + 1
            if (LimeFrameAPI.debugging) println("Starting Execution of Page $newId")
            val newPage = createPage(newId, setting)
            pages[newId] = newPage
            newPage.apply(block)
            if (LimeFrameAPI.debugging) println("Finished Execution of Page $newId")
        }

        if (currentExecutingAction == ChestGuiActions.PAGE_ITEMS) {
            runBlock()
        } else {
            actions += ChestGuiActions.PAGE_ITEMS to runBlock
            if (LimeFrameAPI.debugging) println("Queued Page ${(pages.keys.maxOrNull() ?: ChestGUI.GLOBAL_PAGE_ID) + 1}")
        }
    }

    /**
     * Creates a new page, correctly copying all items and handlers from the global page.
     */
    private fun createPage(pageId: Int, setting: GUISetting): GuiPage {
        val newPage = GuiPageImpl(this, guiHandler, pageId, setting,)

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
        guiHandler.itemClickHandler[ChestGUI.GLOBAL_PAGE_ID]?.forEach { (slot, handler) ->
            val isDynamic = globalPage.trackAddItemSlot.containsKey(slot)

            if (!isDynamic) {
                val pageHandlers = guiHandler.itemClickHandler.computeIfAbsent(pageId) { mutableMapOf() }
                pageHandlers[slot] = handler
            }
        }

        return newPage
    }

    // Item Management
    fun addItem(item: GuiItem?, onClick: (InventoryClickEvent) -> Unit = {}) {
        val runBlock = to@{
            if (item != null) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to
                globalPage.addItem(item, onClick)
            }
        }
        if (currentExecutingAction == ChestGuiActions.GLOBAL_ITEMS) runBlock()
        else actions += ChestGuiActions.GLOBAL_ITEMS to runBlock

    }

    fun addItem(items: List<GuiItem>, onClick: ((InventoryClickEvent) -> Unit) = { _ -> }) {
        val runBlock = to@{
            if (items.isEmpty()) return@to
            val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to

            items.forEach { guiItem ->
                globalPage.addItem(guiItem) { event -> onClick.invoke(event) }
            }
        }
        if (currentExecutingAction == ChestGuiActions.GLOBAL_ITEMS) runBlock()
        else actions += ChestGuiActions.GLOBAL_ITEMS to runBlock
    }

    fun setItem(item: GuiItem?, onClick: (InventoryClickEvent) -> Unit = {}) {
        actions += ChestGuiActions.GLOBAL_ITEMS to {
            if (item != null) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to

                if (item.slot != null) {
                    globalPage.setItem(item.slot!!, item, onClick)
                }

                if (item.slotList.isNotEmpty()) {
                    item.slotList.forEach { slot ->
                        globalPage.setItem(slot, item, onClick)
                    }
                }
            }
        }
    }

    fun setItem(item: GuiItem?, slot: Int?, onClick: (InventoryClickEvent) -> Unit = {}) {
        actions += ChestGuiActions.GLOBAL_ITEMS to {
            if (slot != null && item != null) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to
                globalPage.setItem(slot, item, onClick)
            }
        }
    }

    fun setItem(items: GuiItem?, slot: List<Int>, onClick: ((InventoryClickEvent) -> Unit) = { _ -> }) {
        actions += ChestGuiActions.GLOBAL_ITEMS to {
            if (items != null && slot.isNotEmpty()) {
                val globalPage = pages[ChestGUI.GLOBAL_PAGE_ID] ?: return@to

                slot.forEach { currentSlot ->
                    globalPage.setItem(currentSlot, items) { event -> onClick.invoke(event) }
                }
            }
        }
    }

    fun nav(block: Navigation.() -> Unit) {

        val navigation = Navigation(this, guiHandler).apply(block)


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
    fun build(): GUIEventHandler {
        actions.sortedBy { it.first.priority }.forEach { (action, block) ->
            currentExecutingAction = action
            block()
            currentExecutingAction = null
        }

        pages.forEach { (id, page) ->
            guiHandler.pageInventories[id] = page.inventory
        }

        return guiHandler
    }

}