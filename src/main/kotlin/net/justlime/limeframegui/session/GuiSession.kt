package net.justlime.limeframegui.session

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.menu.GuiPage
import net.justlime.limeframegui.builder.ChestGUIBuilder
import net.justlime.limeframegui.engine.ConditionEngine
import net.justlime.limeframegui.engine.TextResolver
import net.justlime.limeframegui.models.GuiBuffer
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.util.PerformanceMonitor
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.scheduler.BukkitRunnable

class GuiSession(private val blueprint: ChestGUI, val context: GuiStyleSheet) {

    private val viewer = context.viewer
        ?: throw IllegalStateException("Cannot start a GUI Session without a player in the stylesheet context.")
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

        if (builder.pages[0] != null) globalPage = builder.pages[0]
            ?: throw IllegalStateException("Cannot start a GUI Session without a global page in the builder")

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

        // Helper to check requirements with full TextResolver context!
        fun getValidItem(items: List<GuiItem>): GuiItem? {
            return items.sortedByDescending { it.priority }
                .firstOrNull { item ->
                    // 🌟 THIS IS THE FIX: Resolve variables/PAPI before checking the condition!
                    val resolvedReqs = TextResolver.resolveList(viewer, item.viewRequirements, blueprint.setting)
                    ConditionEngine.checkRequirements(viewer, resolvedReqs)
                }
        }

        // 1. Process Global Page
        if (pageId != 0) {
            val globalItemsBySlot = globalPage.getItems().filter { it.slot != null }.groupBy { it.slot!! }

            for ((slot, items) in globalItemsBySlot) {
                if (globalPage.trackAddItemSlot.containsKey(slot)) continue

                val validItem = getValidItem(items)
                if (validItem != null) {
                    val globalStack = ItemRenderer.render(validItem, context, blueprint.setting)
                    styledInventory.setItem(slot, globalStack)
                }
            }
        }

        // 2. Process Current Page
        val localItemsBySlot = guiPage.getItems().filter { it.slot != null }.groupBy { it.slot!! }

        for ((slot, items) in localItemsBySlot) {
            val validItem = getValidItem(items)
            if (validItem != null) {
                val finalItemStack = ItemRenderer.render(validItem, context, blueprint.setting)
                styledInventory.setItem(slot, finalItemStack)
            }
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
        val titleRule = blueprint.setting.style.textSettings.title
        return FontStyle.applyStyle(title, context, titleRule)
    }

    private fun startStateObserver(pageId: Int, inventory: Inventory) {
        activeObserverTask?.cancel()
        val volatileNodes = mutableMapOf<Int, GuiItem>()

        // Helper to check requirements with full TextResolver context!
        fun getActiveItem(items: List<GuiItem>): GuiItem? {
            return items.sortedByDescending { it.priority }
                .firstOrNull { item ->
                    val resolvedReqs = TextResolver.resolveList(viewer, item.viewRequirements, blueprint.setting)
                    ConditionEngine.checkRequirements(viewer, resolvedReqs)
                }
        }

        // 1. Scan Global Page for volatile items
        val globalItemsBySlot = globalPage.getItems().filter { it.slot != null }.groupBy { it.slot!! }
        for ((slot, items) in globalItemsBySlot) {
            if (globalPage.trackAddItemSlot.containsKey(slot)) continue

            val winner = getActiveItem(items)
            if (winner != null && winner.updateInterval != null && winner.updateInterval!! > 0) {
                volatileNodes[slot] = winner
            }
        }

        // 2. Scan Current Page for volatile items
        val localItemsBySlot = builder.pages[pageId]?.getItems()?.filter { it.slot != null }?.groupBy { it.slot!! } ?: emptyMap()
        for ((slot, items) in localItemsBySlot) {
            val winner = getActiveItem(items)
            if (winner != null && winner.updateInterval != null && winner.updateInterval!! > 0) {
                volatileNodes[slot] = winner
            }
        }

        if (volatileNodes.isEmpty()) return

        // 3. Launch the Observer
        activeObserverTask = object : BukkitRunnable() {
            var ticksLived = 0
            override fun run() {
                if (!viewer.isOnline || viewer.openInventory.topInventory != inventory) {
                    cancel()
                    return
                }

                ticksLived++
                for ((slot, blueprintItem) in volatileNodes) {
                    val interval = blueprintItem.updateInterval ?: continue
                    if (ticksLived % interval == 0) {
                        val updatedStack = ItemRenderer.render(blueprintItem, context, blueprint.setting)
                        inventory.setItem(slot, updatedStack)
                    }
                }
            }
        }
        activeObserverTask?.runTaskTimer(LimeFrameAPI.getPlugin(), 1L, 1L)
    }
}
