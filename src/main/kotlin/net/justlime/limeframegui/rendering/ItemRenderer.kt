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

        // Start with the Session Context (Player, Global PAPI)
        val finalContext = sessionContext.copy()

        // Merge Item-Specific Placeholders into the Session Context
        item.style.let { itemContext ->
            val mergedPlaceholders = finalContext.placeholder.toMutableMap()
            mergedPlaceholders.putAll(itemContext.placeholder)
            finalContext.placeholder = mergedPlaceholders

            // Merge other overrides if needed
            if (itemContext.offlinePlayer != null) finalContext.offlinePlayer = itemContext.offlinePlayer
        }

        // Apply the combined context
        tempItem.style = finalContext

        return tempItem.toItemStack()
    }
}