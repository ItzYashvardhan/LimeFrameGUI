package net.justlime.limeframegui.session

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.impl.ChestGUIBuilder
import net.justlime.limeframegui.models.LimeStyleSheet
import net.justlime.limeframegui.rendering.ItemRenderer
import net.justlime.limeframegui.type.ChestGUI
import org.bukkit.Bukkit

/**
 * Represents an active GUI session for a specific player.
 *
 * This class isolates the "Runtime" state from the "Blueprint" state.
 * It is responsible for:
 * 1. Creating the actual Bukkit Inventories with Player-Specific titles.
 * 2. Rendering items with Player-Specific placeholders.
 * 3. Connecting the inventories to the Event Handler.
 */
class GuiSession(private val blueprint: ChestGUI, private val context: LimeStyleSheet) {

    private val player = context.player ?: throw IllegalStateException("Cannot start a GUI Session without a player in the stylesheet context.")

    /**
     * Starts the session: Builds the menu and opens it for the player.
     * @param initialPage The page ID to open first.
     */
    fun start(initialPage: Int? = null) {
        val builder = ChestGUIBuilder(blueprint.setting)
        builder.apply(blueprint.block)
        val handler = builder.build()

        // Get reference to Global Page (Page 0)
        val globalPage = builder.pages[0]
        builder.pages.forEach { (pageId, guiPage) ->

            val styledTitle = generateTitle(blueprint.setting.title, pageId)
            val size = blueprint.setting.rows * 9
            val styledInventory = Bukkit.createInventory(handler, size, styledTitle)

            // INHERITANCE LOGIC
            // When building Page 1, 2, etc., paint Global Items FIRST
            if (pageId != 0 && globalPage != null) {
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

        // Smart Page Selection
        val minPageId = if (builder.pages.size == 1) 0 else builder.pages.keys.filter { it != 0 }.minOrNull() ?: 1
        val finalPageId = initialPage ?: minPageId
        handler.open(player, finalPageId)
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