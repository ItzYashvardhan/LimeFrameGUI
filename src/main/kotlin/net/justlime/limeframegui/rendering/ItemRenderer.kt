package net.justlime.limeframegui.rendering

import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.LimeStyleSheet
import org.bukkit.inventory.ItemStack

/**
 * Responsible for converting a blueprint "GuiItem" into a Bukkit "ItemStack".
 * It injects the context (Player/PAPI) at the last possible moment.
 */
object ItemRenderer {

    fun render(item: GuiItem, sessionContext: LimeStyleSheet): ItemStack {
        val tempItem = item.clone()

        // 1. Start with the Session Context (Player, Global PAPI)
        val finalContext = sessionContext.copy()

        // 2. FIX: Merge Item-Specific Placeholders into the Session Context
        // If the item has its own stylesheet (like your item4 with {world}), add those keys.
        item.styleSheet?.let { itemContext ->
            val mergedPlaceholders = finalContext.placeholder.toMutableMap()
            mergedPlaceholders.putAll(itemContext.placeholder)
            finalContext.placeholder = mergedPlaceholders

            // Merge other overrides if needed
            if (itemContext.offlinePlayer != null) finalContext.offlinePlayer = itemContext.offlinePlayer
        }

        // 3. Apply the combined context
        tempItem.styleSheet = finalContext

        return tempItem.toItemStack()
    }
}