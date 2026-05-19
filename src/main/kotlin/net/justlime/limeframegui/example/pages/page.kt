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

package net.justlime.limeframegui.example.pages

import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.util.toGuiItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

fun pageExample(player: Player) {

    val nextItem = ItemStack(Material.ARROW).toGuiItem()
    nextItem.name = "next"
    val prevItem = ItemStack(Material.ARROW).toGuiItem()
    prevItem.name = "prev"

    val item1 = ItemStack(Material.PAPER).toGuiItem()
    val item2 = ItemStack(Material.DIAMOND).toGuiItem()
    val item3 = ItemStack(Material.STONE).toGuiItem()
    val item4 = ItemStack(Material.IRON_SWORD).toGuiItem()

    ChestGUI(6, "Pager GUI") {

        this.nav {
            this.nextItem = nextItem
            this.prevItem = prevItem
            this.margin = 3
//                this.nextSlot = 48
//                this.prevSlot = 51

            buffer {}
        }

        //Global Click handler
        onClick { it.isCancelled = true }

        //This item added to every page
        //You can used it as Custom Background Design
        item4.slot = 5
        setItem(item4) {
            it.whoClicked.sendMessage("You click on global item")
        }
        addPage(GuiSetting(6, "Regular Page {page}")) {
            //this item added to specific page only (page 1)
            for (i in 1..100) {
                val newItem = item1.copy(name = "Item $i")
                addItem(newItem) {
                    it.whoClicked.sendMessage("Removed Item at ${it.currentItem?.itemMeta?.displayName}")
                    remove(it.slot)
                }
            }

            //Runs for only specific Page (1)
            onOpen {
                player.sendMessage("You open a page 1")
            }
        }

    }.open(player)
}