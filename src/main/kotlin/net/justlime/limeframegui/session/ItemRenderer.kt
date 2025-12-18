/*
 * RedeemCodeX - Plugin License Agreement
 * Copyright © 2024 Yashvardhan
 *
 * This software is a paid plugin developed by Yashvardhan ("Author") and is provided to you ("User") under the following terms:
 *
 * 1. Usage Rights:
 *    - This plugin is licensed, not sold.
 *    - One license grants usage on **one server network only**, unless explicitly agreed otherwise.
 *    - You may not sublicense, share, leak, or resell the plugin or any part of it.
 *
 * 2. Restrictions:
 *    - You may not decompile, reverse engineer, or modify the plugin.
 *    - You may not redistribute the plugin in any form.
 *    - You may not upload this plugin to any public or private repository or distribution platform.
 *
 * 3. Support & Updates:
 *    - Support is provided to verified buyers only.
 *    - Updates are available as long as development continues or within the support duration stated at purchase.
 *
 * 4. Termination:
 *    - Any violation of this agreement terminates your rights to use this plugin immediately, without refund.
 *
 * 5. No Warranty:
 *    - The plugin is provided "as is", without warranty of any kind. Use at your own risk.
 *    - The Author is not responsible for any damages, data loss, or server issues resulting from usage.
 *
 * For inquiries,
 * Email: itsyashvardhan76@gmail.com
 * Discord: https://discord.gg/rVsUJ4keZN
 */

package net.justlime.limeframegui.session

import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import org.bukkit.inventory.ItemStack

/**
 * Responsible for converting a blueprint "GuiItem" into a Bukkit "ItemStack".
 * It injects the context (Player/PAPI) at the last possible moment.
 */
object ItemRenderer {

    fun render(item: GuiItem, sessionContext: GuiStyleSheet): ItemStack {
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