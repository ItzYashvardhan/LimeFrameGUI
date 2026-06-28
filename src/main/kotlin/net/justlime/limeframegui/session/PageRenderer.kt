package net.justlime.limeframegui.session

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.engine.ConditionEngine
import net.justlime.limeframegui.engine.TextResolver
import net.justlime.limeframegui.menu.GuiPage
import net.justlime.limeframegui.models.GuiItem
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory

/**
 * Responsible for assembling a live Bukkit Inventory from GuiPage blueprints.
 */
object PageRenderer {

    fun render(
        session: GuiSession,
        pageId: Int,
        guiPage: GuiPage,
        reuseExisting: Boolean = false
    ): Inventory {
        val handler = session.handler
        val blueprint = session.blueprint
        val context = session.styleSheet
        val viewer = context.viewer ?: return Bukkit.createInventory(handler, 9, "Error")

        // Prepare the Inventory
        val existingInv = handler.pageInventories[pageId]
        val styledInventory = if (reuseExisting && existingInv != null) {
            existingInv.clear()
            existingInv
        } else {
            createStylishInventory(session, pageId)
        }

        // Helper to filter items by requirements and priority
        fun getValidItem(items: List<GuiItem>): GuiItem? {
            return items.sortedByDescending { it.priority }
                .firstOrNull { item ->
                    val resolvedReqs = TextResolver.resolveList(viewer, item.viewRequirements, blueprint.setting)
                    ConditionEngine.checkRequirements(viewer, resolvedReqs)
                }
        }

        // Inject Global Page Items (if not the global page itself)
        if (pageId != 0) {
            val globalItemsBySlot = session.globalPage.getItems().filter { it.slot != null }.groupBy { it.slot!! }

            for ((slot, items) in globalItemsBySlot) {
                if (session.globalPage.trackAddItemSlot.containsKey(slot)) continue

                val validItem = getValidItem(items)
                if (validItem != null) {
                    val globalStack = ItemRenderer.render(validItem, context, blueprint.setting)
                    styledInventory.setItem(slot, globalStack)
                }
            }
        }

        // Inject Local Current Page Items
        val localItemsBySlot = guiPage.getItems().filter { it.slot != null }.groupBy { it.slot!! }

        for ((slot, items) in localItemsBySlot) {
            val validItem = getValidItem(items)
            if (validItem != null) {
                val finalItemStack = ItemRenderer.render(validItem, context, blueprint.setting)
                styledInventory.setItem(slot, finalItemStack)
            }
        }

        // Update the Page State
        handler.pageInventories[pageId] = styledInventory
        guiPage.inventory = styledInventory
        guiPage.isRendered = true

        return styledInventory
    }

    private fun createStylishInventory(session: GuiSession, pageId: Int): Inventory {
        val setting = session.blueprint.setting
        val styledTitle = generateTitle(session, setting.title, pageId)
        val size = setting.rows * 9
        return Bukkit.createInventory(session.handler, size, styledTitle)
    }

    private fun generateTitle(session: GuiSession, rawTitle: String, pageId: Int): String {
        val context = session.styleSheet
        val setting = session.blueprint.setting
        
        val title = if (context.placeholder["{page}"] == null) {
            rawTitle.replace("{page}", pageId.toString())
        } else rawTitle
        
        val titleRule = setting.style.textSettings.title
        return FontStyle.applyStyle(title, context, titleRule)
    }
}