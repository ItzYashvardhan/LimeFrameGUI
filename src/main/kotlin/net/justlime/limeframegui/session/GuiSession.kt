package net.justlime.limeframegui.session

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.handler.GuiEventHandler
import net.justlime.limeframegui.handler.GuiPage
import net.justlime.limeframegui.impl.ChestGUIBuilder
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.type.ChestGUI
import net.justlime.limeframegui.utilities.PerformanceMonitor
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory

/**
 * Represents an active GUI session for a specific player.
 *
 * This class isolates the "Runtime" state from the "Blueprint" state.
 * It is responsible for:
 * 1. Creating the actual Bukkit Inventories with Player-Specific titles.
 * 2. Rendering items with Player-Specific placeholders.
 * 3. Connecting the inventories to the Event Handler.
 */
class GuiSession(private val blueprint: ChestGUI,  val context: GuiStyleSheet) {

    private val viewer = context.viewer ?: throw IllegalStateException("Cannot start a GUI Session without a player in the stylesheet context.")
    private var buffer: GuiBuffer? = null
    lateinit var handler: GuiEventHandler
    lateinit var globalPage: GuiPage
    lateinit var builder: ChestGUIBuilder

    /**
     * Starts the session: Builds the menu and opens it for the player.
     * @param initialPage The page ID to open first.
     */
    fun start(initialPage: Int? = null) {
        builder = ChestGUIBuilder(this, blueprint.setting)
        builder.apply(blueprint.block)
        buffer = builder.buffer
        handler = builder.build()

        if (builder.pages[0] != null) globalPage = builder.pages[0] ?: throw IllegalStateException("Cannot start a GUI Session without a global page in the builder")

        // Smart Page Selection
        val minPageId = if (builder.pages.size == 1) 0 else builder.pages.keys.filter { it != 0 }.minOrNull() ?: 1
        val finalPageId = initialPage ?: minPageId

        // Render Pages
        if (buffer == null) PerformanceMonitor.measure("FULL PAGES") {
            builder.pages.forEach { (pageId, guiPage) ->
                renderPage(pageId, guiPage)
            }
        }

        // Lazy Render
        if (buffer != null) PerformanceMonitor.measure("LAZY PAGES") {
            renderBufferPages(finalPageId)
        }


        handler.open(viewer, finalPageId)

    }

    fun startAsync(initialPage: Int? = null){
        builder = ChestGUIBuilder(this, blueprint.setting)
        builder.apply(blueprint.block)
        buffer = builder.buffer
        handler = builder.build()

        if (builder.pages[0] != null) globalPage = builder.pages[0] ?: throw IllegalStateException("Cannot start a GUI Session without a global page in the builder")

        // Smart Page Selection
        val minPageId = if (builder.pages.size == 1) 0 else builder.pages.keys.filter { it != 0 }.minOrNull() ?: 1
        val finalPageId = initialPage ?: minPageId

        // Render Pages
        if (buffer == null) PerformanceMonitor.measure("FULL PAGES") {
            builder.pages.forEach { (pageId, guiPage) ->
                renderPage(pageId, guiPage)
            }
        }

        // Lazy Render
        if (buffer != null) PerformanceMonitor.measure("LAZY PAGES") {
            renderBufferPages(finalPageId)
        }

        handler.open(viewer, finalPageId)
    }

    fun bufferPage(pageId: Int) {
        val buffer = this@GuiSession.buffer ?: run {
            handler.open(viewer, pageId)
            return
        }

        PerformanceMonitor.measure("LAZY PAGE ${handler.getCurrentPage(viewer)}") {
            val allPageIds = builder.pages.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }.sorted()
            val requestedIndex = allPageIds.indexOf(pageId)
            if (requestedIndex == -1) return@measure

            // FORWARD LOGIC
            // Find the highest index that is currently rendered
            val currentMaxIndex = allPageIds.indices.reversed().find { idx ->
                val inv = handler.pageInventories[allPageIds[idx]]
                inv != null && !inv.isEmpty
            } ?: requestedIndex

            // Trigger if we are within the margin of the last loaded page
            val shouldLoadMoreForward = (requestedIndex + buffer.margin) >= currentMaxIndex
            if (shouldLoadMoreForward) {
                val start = currentMaxIndex + 1
                val end = (currentMaxIndex + buffer.renderLimit).coerceAtMost(allPageIds.size - 1)
                renderRange(start..end, allPageIds)
            }

            // BACKWARD LOGIC
            // Find the lowest index that is currently rendered
            val currentMinIndex = allPageIds.indices.find { idx ->
                val inv = handler.pageInventories[allPageIds[idx]]
                inv != null && !inv.isEmpty
            } ?: requestedIndex

            // Trigger if we are within the margin of the first loaded page
            val shouldLoadMoreBackward = (requestedIndex - buffer.margin) <= currentMinIndex
            if (shouldLoadMoreBackward) {
                // Calculate the chunk backwards
                val end = currentMinIndex - 1
                val start = (currentMinIndex - buffer.renderLimit).coerceAtLeast(0)
                renderRange(start..end, allPageIds)
            }
            // Cleanup logic (Memory Management)
            if (buffer.cleanupMargin >= 0) {
                val keepRange = (requestedIndex - buffer.cleanupMargin)..(requestedIndex + buffer.cleanupMargin)
                val currentlyLoadedIds = handler.pageInventories.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }

                currentlyLoadedIds.forEach { loadedId ->
                    val loadedIndex = allPageIds.indexOf(loadedId)
                    if (loadedIndex !in keepRange) {
                        handler.pageInventories[loadedId]?.clear()
                        handler.pageInventories.remove(loadedId)
                        builder.pages[loadedId]?.inventory?.clear()
                    }
                }
            }

            builder.pages[ChestGUI.GLOBAL_PAGE_ID]?.let {
                if (handler.pageInventories[ChestGUI.GLOBAL_PAGE_ID] == null) renderPage(ChestGUI.GLOBAL_PAGE_ID, it)
            }
        }

        PerformanceMonitor.measure("Open Handler") {
            handler.open(viewer, pageId)
        }
    }

    private fun renderRange(range: IntRange, allPageIds: List<Int>) {
        if (range.first > range.last) return
        range.forEach { index ->
            val id = allPageIds[index]
            val guiPage = builder.pages[id] ?: return@forEach
            val inv = handler.pageInventories[id]
            if (inv == null || inv.isEmpty) {
                renderPage(id, guiPage)
            }
        }
    }

    private fun renderBufferPages(finalPageId: Int) {
        //Lazy Render
        val pagesToRender = mutableSetOf<Int>()

        // Always render the global page
        pagesToRender.add(ChestGUI.GLOBAL_PAGE_ID)

        // Always render the initial page
        pagesToRender.add(finalPageId)

        // Render pages around the initial page based on renderLimit
        val allPageIds = builder.pages.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }.sorted()
        val initialPageIndex = allPageIds.indexOf(finalPageId)

        if (initialPageIndex != -1) {
            val start = (initialPageIndex - buffer!!.renderLimit).coerceAtLeast(0)
            val end = (initialPageIndex + buffer!!.renderLimit).coerceAtMost(allPageIds.size - 1)

            for (i in start..end) {
                pagesToRender.add(allPageIds[i])
            }
        }

        pagesToRender.forEach { pageId ->
            builder.pages[pageId]?.let { guiPage ->
                renderPage(pageId, guiPage)
            }
        }
    }

    private fun renderPage(pageId: Int, guiPage: GuiPage) {
        val styledInventory = createStylishInventory(pageId)

        // INHERITANCE LOGIC
        // When building Page 1, 2, etc., paint Global Items FIRST
        if (pageId != 0) {
            globalPage.getItems().forEach { (slot, guiItem) ->

                // Only copy items that are STATIC (e.g.,Global Items, Nav Buttons).
                val isDynamic = globalPage.trackAddItemSlot.containsKey(slot)

                if (!isDynamic) {
                    val globalStack = ItemRenderer.render(guiItem, context)
                    styledInventory.setItem(slot, globalStack)
                }
            }
        }

        // PAGE SPECIFIC LOGIC
        // Paint the Page-Specific items (These will overwrite global items if they clash)
        guiPage.getItems().forEach { (slot, guiItem) ->
            val finalItemStack = ItemRenderer.render(guiItem, context)
            styledInventory.setItem(slot, finalItemStack)
        }

        handler.pageInventories[pageId] = styledInventory
        guiPage.inventory = styledInventory
    }

    private fun createStylishInventory(pageId: Int): Inventory {
        //Create New Inventory
        val styledTitle = generateTitle(blueprint.setting.title, pageId)
        val size = blueprint.setting.rows * 9
        val styledInventory = Bukkit.createInventory(handler, size, styledTitle)
        return styledInventory
    }

    /**
     * Helper to process the Window Title with placeholders and colors.
     */
    private fun generateTitle(rawTitle: String, pageId: Int): String {
        val title = rawTitle.replace("{page}", pageId.toString())
        val useStylishFont = blueprint.setting.style.stylishTitle
        return FontStyle.applyStyle(title, context, useStylishFont)
    }
}