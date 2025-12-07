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
class GuiSession(
    private val blueprint: ChestGUI, private val context: LimeStyleSheet
) {

    // We ensure the context has a player attached, as a Session requires a viewer.
    private val player = context.player ?: throw IllegalStateException("Cannot start a GuiSession without a player in the stylesheet context.")

    /**
     * Starts the session: Builds the menu and opens it for the player.
     * @param initialPage The page ID to open first (default is usually 0).
     */
    fun start(initialPage: Int = 0) {
        val builder = ChestGUIBuilder(blueprint.setting)
        builder.apply(blueprint.block)
        val handler = builder.build()

        // 1. Get reference to Global Page items (Page 0)
        val globalPage = builder.pages[0]

        builder.pages.forEach { (pageId, guiPage) ->

            val styledTitle = generateTitle(blueprint.setting.title, pageId)
            val size = blueprint.setting.rows * 9
            val styledInventory = Bukkit.createInventory(handler, size, styledTitle)

            // --- FIX START: Copy Global Items ---
            // If we are building Page 1, 2, etc., we must first paint the Global Items
            if (pageId != 0 && globalPage != null) {
                globalPage.getItems().forEach { (slot, guiItem) ->
                    // Render global items using the current session context
                    val globalStack = ItemRenderer.render(guiItem, context)
                    styledInventory.setItem(slot, globalStack)
                }
            }
            // --- FIX END ---

            // 2. Now paint the Page-Specific items (These will overwrite global items if they clash)
            guiPage.getItems().forEach { (slot, guiItem) ->
                val finalItemStack = ItemRenderer.render(guiItem, context)
                styledInventory.setItem(slot, finalItemStack)
            }

            handler.pageInventories[pageId] = styledInventory
            guiPage.inventory = styledInventory
        }

        handler.open(player, initialPage)
    }
    /**
     * Helper to process the Window Title with placeholders and colors.
     */
    private fun generateTitle(rawTitle: String, pageId: Int): String {
        // 1. Handle {page} placeholder locally
        val step1 = rawTitle.replace("{page}", pageId.toString())

        // 2. Handle Colors, PAPI, and SmallCaps using your existing logic
        // We use the 'context' (LimeStyleSheet) specific to this player.
        return FontStyle.applyStyle(
            step1, context, blueprint.setting.styleSheet?.stylishTitle ?: false // Fallback to blueprint setting
        )
    }
}