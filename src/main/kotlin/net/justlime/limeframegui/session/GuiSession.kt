package net.justlime.limeframegui.session

import net.justlime.limeframegui.api.LimeFrameAPI
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

class GuiSession(private val blueprint: ChestGUI, val context: GuiStyleSheet) {

    private val viewer = context.viewer ?: throw IllegalStateException("Cannot start a GUI Session without a player in the stylesheet context.")
    private var buffer: GuiBuffer? = null
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
    }

    fun bufferPage(pageId: Int) {
        val buffer = this@GuiSession.buffer ?: run {
            handler.open(viewer, pageId)
            return
        }

        PerformanceMonitor.measure("LAZY PAGE ${handler.getCurrentPage(viewer)}") {
            val allPageIds = builder.pages.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }.sorted()
            println(allPageIds)
            val requestedIndex = allPageIds.indexOf(pageId)
            if (requestedIndex == -1) return@measure

            val currentMaxIndex = allPageIds.indices.reversed().find { idx ->
                builder.pages[allPageIds[idx]]?.isRendered == true
            } ?: requestedIndex
            println("RequestedIndex: $requestedIndex")
            println("CurrentMaxIndex: $currentMaxIndex")

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
            println("vid $currentPageId")
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
        val pagesToRender = mutableSetOf<Int>()
        pagesToRender.add(ChestGUI.GLOBAL_PAGE_ID)
        pagesToRender.add(finalPageId)

        val allPageIds = builder.pages.keys.filter { it != ChestGUI.GLOBAL_PAGE_ID }.sorted()
        val initialPageIndex = allPageIds.indexOf(finalPageId)

        if (initialPageIndex != -1) {
            val start = (initialPageIndex - buffer!!.renderLimit).coerceAtLeast(0)
            val end = (initialPageIndex + buffer!!.renderLimit).coerceAtMost(allPageIds.size - 1)

            for (i in start..end) {
                pagesToRender.add(allPageIds[i])
            }
        }

        println(pagesToRender)
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
        val title = rawTitle.replace("{page}", pageId.toString())
        val useStylishFont = blueprint.setting.style.stylishTitle
        return FontStyle.applyStyle(title, context, useStylishFont)
    }
}
