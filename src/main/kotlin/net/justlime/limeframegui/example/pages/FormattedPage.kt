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

import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.util.item
import net.justlime.limeframegui.util.update
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


fun formattedPage(setting: GuiSetting, player: Player) {

    setting.style.placeholder["{world}"] = player.world.name + " at " + player.location.x.toInt() + player.location.y.toInt() + player.location.z.toInt()
    ChestGUI(setting) {

        val item3 = GuiItem(
            ItemStack(Material.PLAYER_HEAD), name = "Player: %player_name%", lore = listOf(
                "<green>Playtime Stats1: %statistic_time_played%", "<green>World: <white>{world}</white>", "<aqua>Click to refresh"
            ), texture = "%player_name%"

        )

        val item31 = GuiItem(
            ItemStack(Material.PLAYER_HEAD), name = "Player: %player_name%", lore = listOf(
                "<green>Playtime Stats2: %statistic_time_played%", "<aqua>Click to refresh"
            ), texture = "%player_name%"

        )


        val item1 = GuiItem(
            ItemStack(Material.PAPER), name = "<gradient:red:blue>This is a Gradient title</gradient>", lore = listOf(
                "<red>This is a red line</red>", "<green>This is a green line</green>", "<blue>This is a blue line</blue>"
            )
        )

        addItem(item1) {
            it.whoClicked.sendMessage("Clicked formatted item!")
        }

        setItem(item1, 12) {
            it.whoClicked.sendMessage("Clicked formatted item on ${it.slot}!")
        }

        onClick { it.isCancelled = true }
        addPage {

            val item12 = GuiItem(
                ItemStack(Material.PAPER), name = "<gradient:red:blue>This is a Gradient title</gradient>", lore = listOf(
                    "<red>This is a red line</red>", "<green>This is a green line</green>", "<blue>This is a blue line</blue>"
                )
            )

            addItem(item12) {
                it.whoClicked.sendMessage("Clicked formatted item!")
            }

            val item2 = GuiItem(
                ItemStack(Material.GOLD_INGOT), name = "Player: %betterteams_name%", lore = listOf(
                    "<gold>Balance: %vault_eco_balance%", "<white>Location: %player_x%, %player_y%, %player_z%", "<white>PlayTime: <b>%statistic_time_played% </b>",//
                    "<white> {world}", "custom: {time}"
                )
            )

            val item4 = GuiItem(
                ItemStack(Material.TOTEM_OF_UNDYING), name = "<#FF00FF>Custom PlaceHolder</#FF00FF>", lore = listOf(
                    "<gray>World: {world}</gray>", "<gray>Location: {location}</gray>"

                ), style = setting.style.copy(
                    placeholder = mutableMapOf(
                        "{world}" to player.world.name, "{location}" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}"
                    )
                )
            )

            val item5 = GuiItem(ItemStack(Material.PLAYER_HEAD), texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQzMDhhZTI3YjU4YjY5NjQ1NDk3ZjlkYTg2NTk3ZWRhOTQ3ZWFjZDEwYzI5ZTNkNGJiZjNiYzc2Y2ViMWVhYiJ9fX0=")

            item2.style.placeholder = mutableMapOf("{time}" to player.ticksLived.toString())
            addItem(item2) { event ->
                event.item?.style?.placeholder = mutableMapOf("{time}" to player.ticksLived.toString())
                event.update(session.context,setting)
            }

            addItem(item4)
            addItem(item5)
        }
    }.open(player, 1)

}
