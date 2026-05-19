package net.justlime.limeframegui.session

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.menu.GuiPage
import net.justlime.limeframegui.builder.ChestGUIBuilder
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.util.PerformanceMonitor
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.scheduler.BukkitRunnable

class GuiSession(private val blueprint: ChestGUI, val context: GuiStyleSheet) {

    private val viewer = context.viewer ?: throw IllegalStateException("Cannot start a GUI Session without a player in the stylesheet context.")
    private var buffer: GuiBuffer? = null
    private var activeObserverTask: BukkitRunnable? = null
    lateinit var handler: GuiEventHandler
    lateinit var globalPage: GuiPage
    lateinit var builder: ChestGUIBuilder

    fun start(initialPage: Int? = null) {
        builder = ChestGUIBuilder(this, blueprint.setting)
        builder.apply(blueprint.block)
        buffer = builder.buffer
        handler = builder.build()

        if (builder.pages[0] != null) globalPage = builder.pages[0] ?: throw IllegalStateException("Cannot start a GUI Session without a global page in the builder")

        val minPageId = if (builder.pages.size == 1) 0 else builder.pages.keys.filter { it != 0 }.minOrNull() ?: 1
        val finalPageId = initialPage ?: minPageId

        if (buffer == null) PerformanceMonitor.measure("FULL PAGES") {
            builder.pages.forEach { (pageId, guiPage) ->
                renderPage(pageId, guiPage)
            }
        }

        if (buffer != null) PerformanceMonitor.measure("LAZY PAGES") {
            renderBufferPages(finalPageId)
        }

        handler.open(viewer, finalPageId)
        handler.pageInventories[finalPageId]?.let { activeInv ->
            startStateObserver(finalPageId, activeInv)
        }
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

            val currentMaxIndex = allPageIds.indices.reversed().find { idx ->
                builder.pages[allPageIds[idx]]?.isRendered == true
            } ?: requestedIndex

            val shouldLoadMoreForward = (requestedIndex + buffer.margin) >= currentMaxIndex
            if (shouldLoadMoreForward) {
                val start = currentMaxIndex + 1
                val end = (currentMaxIndex + buffer.renderLimit).coerceAtMost(allPageIds.size - 1)
                renderRange(start..end, allPageIds)
            }

            val currentMinIndex = allPageIds.indices.find { idx ->
                builder.pages[allPageIds[idx]]?.isRendered == true
            } ?: requestedIndex

            val shouldLoadMoreBackward = (requestedIndex - buffer.margin) <= currentMinIndex
            if (shouldLoadMoreBackward) {
                val end = currentMinIndex - 1
                val start = (currentMinIndex - buffer.renderLimit).coerceAtLeast(0)
                renderRange(start..end, allPageIds)
            }
            if (buffer.cleanupMargin >= 0) {
                val keepRange = (requestedIndex - buffer.cleanupMargin)..(requestedIndex + buffer.cleanupMargin)
                val currentlyLoadedIds = handler.pageInventories.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }

                currentlyLoadedIds.forEach { loadedId ->
                    val loadedIndex = allPageIds.indexOf(loadedId)
                    if (loadedIndex !in keepRange) {
                        handler.pageInventories.remove(loadedId)
                        builder.pages[loadedId]?.let {
                            it.inventory.clear()
                            it.isRendered = false
                        }
                    }
                }
            }

            builder.pages[ChestGUI.GLOBAL_PAGE_ID]?.let {
                if (handler.pageInventories[ChestGUI.GLOBAL_PAGE_ID] == null) renderPage(ChestGUI.GLOBAL_PAGE_ID, it)
            }
        }

        PerformanceMonitor.measure("Open Handler") {
            handler.open(viewer, pageId)
            handler.pageInventories[pageId]?.let { activeInv ->
                startStateObserver(pageId, activeInv)
            }
        }
    }

    fun softRefresh() {
        PerformanceMonitor.measure("SOFT REFRESH PAGES") {
            val currentPageId = builder.session.handler.getCurrentPage(viewer) ?: 0
            if (LimeFrameAPI.debugging) println("Soft Refresh PageId used $currentPageId")

            if (buffer != null) {
                renderBufferPages(currentPageId)
            } else {
                builder.pages.forEach { (pageId, guiPage) ->
                    renderPage(pageId, guiPage)
                }
            }
        }
    }

    fun refresh() {
        PerformanceMonitor.measure("REFRESH PAGES") {
            val currentPageId = builder.session.handler.getCurrentPage(viewer) ?: 0
            if (LimeFrameAPI.debugging) println("Refresh PageId used $currentPageId")
            if (buffer == null) {
                builder.pages.forEach { (pageId, guiPage) ->
                    renderPage(pageId, guiPage)
                }
                return
            }
            renderBufferPages(currentPageId)
            handler.open(viewer, currentPageId)
        }
    }

    private fun renderRange(range: IntRange, allPageIds: List<Int>) {
        if (range.first > range.last) return
        range.forEach { index ->
            val id = allPageIds[index]
            val guiPage = builder.pages[id] ?: return@forEach
            if (!guiPage.isRendered) {
                renderPage(id, guiPage)
            }
        }
    }

    private fun renderBufferPages(finalPageId: Int) {
        val currentBuffer = buffer ?: return
        val pagesToRender = mutableSetOf<Int>()
        pagesToRender.add(ChestGUI.GLOBAL_PAGE_ID)
        pagesToRender.add(finalPageId)

        val allPageIds = builder.pages.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }.sorted()
        val initialPageIndex = allPageIds.indexOf(finalPageId)

        if (initialPageIndex != -1) {
            val start = (initialPageIndex - currentBuffer.renderLimit).coerceAtLeast(0)
            val end = (initialPageIndex + currentBuffer.renderLimit).coerceAtMost(allPageIds.size - 1)

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

        if (pageId != 0) {
            globalPage.getItems().forEach { (slot, guiItem) ->
                val isDynamic = globalPage.trackAddItemSlot.containsKey(slot)
                if (!isDynamic) {
                    val globalStack = ItemRenderer.render(guiItem, context)
                    styledInventory.setItem(slot, globalStack)
                }
            }
        }

        guiPage.getItems().forEach { (slot, guiItem) ->
            val finalItemStack = ItemRenderer.render(guiItem, context)
            styledInventory.setItem(slot, finalItemStack)
        }

        handler.pageInventories[pageId] = styledInventory
        guiPage.inventory = styledInventory
        guiPage.isRendered = true
    }

    private fun createStylishInventory(pageId: Int): Inventory {
        val styledTitle = generateTitle(blueprint.setting.title, pageId)
        val size = blueprint.setting.rows * 9
        return Bukkit.createInventory(handler, size, styledTitle)
    }

    private fun generateTitle(rawTitle: String, pageId: Int): String {
        val title = if (context.placeholder["{page}"] == null) {
            rawTitle.replace("{page}", pageId.toString())
        } else rawTitle
        val useStylishFont = blueprint.setting.style.stylishTitle
        return FontStyle.applyStyle(title, context, useStylishFont)
    }

    private fun startStateObserver(pageId: Int, inventory: Inventory) {
        // Cancel any existing observer (e.g., if they just turned the page)
        activeObserverTask?.cancel()

        val volatileNodes = mutableMapOf<Int, GuiItem>()

        // 1. Scan Global Page for volatile items
        globalPage.getItems().forEach { (slot, item) ->
            if (item.updateInterval != null && item.updateInterval!! > 0) {
                volatileNodes[slot] = item
            }
        }

        // 2. Scan Current Page for volatile items (overwrites global if overlapping)
        builder.pages[pageId]?.getItems()?.forEach { (slot, item) ->
            if (item.updateInterval != null && item.updateInterval!! > 0) {
                volatileNodes[slot] = item
            }
        }

        // If nothing needs observing, don't start the task!
        if (volatileNodes.isEmpty()) return

        // 3. Launch the Observer
        activeObserverTask = object : BukkitRunnable() {
            var ticksLived = 0

            override fun run() {
                // Self-Destruct if player went offline or closed the GUI
                if (!viewer.isOnline || viewer.openInventory.topInventory != inventory) {
                    cancel()
                    return
                }

                ticksLived++

                // Recompose items when their specific interval hits
                for ((slot, blueprintItem) in volatileNodes) {
                    val interval = blueprintItem.updateInterval ?: continue
                    if (ticksLived % interval == 0) {
                        // Use your existing ItemRenderer to process PAPI and Context safely!
                        val updatedStack = ItemRenderer.render(blueprintItem, context)
                        inventory.setItem(slot, updatedStack)
                    }
                }
            }
        }

        // Start running every 1 tick
        activeObserverTask?.runTaskTimer(LimeFrameAPI.getPlugin(), 1L, 1L)
    }
}
