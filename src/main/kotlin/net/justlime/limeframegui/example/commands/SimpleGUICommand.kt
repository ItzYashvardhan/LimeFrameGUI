package net.justlime.limeframegui.example.commands

import net.justlime.limeframegui.example.ExampleFrameManager
import net.justlime.limeframegui.example.pages.pageExample
import net.justlime.limeframegui.example.pages.simpleGUI
import net.justlime.limeframegui.manager.GuiManager
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.util.item
import net.justlime.limeframegui.util.toGuiItem
import net.justlime.limeframegui.util.update
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class SimpleGUICommand() : CommandHandler {
    override val permission: String = ""
    override val aliases: List<String> = mutableListOf()

    override fun onCommand(
        sender: CommandSender, command: Command, label: String, args: Array<out String?>
    ): Boolean {
        if (sender !is Player) {
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("No arguments provided")
            return true
        }


        when (args[0]) {
            "example" ->{
                GuiManager.open(sender, "example")
                sender.sendMessage("§aOpening the example GUI...")
            }

//            "save" -> savePage(sender)
            "page" -> {
                pageExample(sender)
            }

            "home" -> {
                homePage(sender)
            }

            "nested" -> {
                nestedPage(sender)
            }

            "formatted" -> {
                ExampleFrameManager.openFormattedGUI(sender)
            }

            "async" -> {
                ExampleFrameManager.openAsync(sender)
            }

            else -> {}
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String?>
    ): List<String?> {
        val completion = mutableListOf<String>()
        if (args.isNotEmpty()) completion.addAll(listOf("example", "page", "home", "nested", "formatted", "async"))
        return completion
    }


    fun homePage(player: Player) {

        ChestGUI(1, "hello %player_name%") {

            var value = 0

            onClose {
                it.player.sendMessage("Closing Inventory")
            }

            onClick { it.isCancelled = true }

            val simpleItem =
                ItemStack(Material.GRASS_BLOCK).toGuiItem().apply { name = "Open Simple GUI for %player_name%" }

            addItem(simpleItem) {
                simpleGUI().open(it.whoClicked as Player)
            }

            val pageItem = ItemStack(Material.BOOK).toGuiItem().apply { name = "Open Pager GUI"; }

            val staticExtraItem =
                GuiItem(Material.PAPER, name = "Entered $value", lore = listOf("§aPlayTime: %statistic_time_played%"))
            val dynamicExtraItem = GuiItem(
                Material.PAPER,
                nameState = { "Entered $value" },
                loreState = { listOf("§aPlayTime: %statistic_time_played%") })
            addItem(pageItem) {
                pageExample(player)
            }

            val items = mutableListOf(staticExtraItem, dynamicExtraItem).toList()

            addItem(items) { item, event ->
                event.whoClicked.sendMessage("You click on ${event.item?.name}")
                event.whoClicked.sendMessage("You click on ${event.item?.currentName}")
                when (event.click) {
                    ClickType.LEFT -> {
                        value++
                    }

                    ClickType.RIGHT -> {
                        value--
                    }

                    else -> {
                        value = -1
                    }
                }

                event.update()

            }

            //The only difference in between them that static doesn't point to current variable state where dynamic does!

        }.open(player)

    }

    fun nestedPage(player: Player) {

        //Useful if you gui can have various different page (Great for Tree Like Structure)
        //Don't use nav{} //It will give unexpected behaviour

        ChestGUI(6, "Nested GUI") {
            onClick { it.isCancelled = true }

            addPage(GuiSetting(6, "Nested Page 1")) {
                val item1 = ItemStack(Material.PAPER).toGuiItem()
                item1.name = "Go to Nested Page 2"
                addItem(item1) {
                    openPage(it.whoClicked as Player, 2)
                }

                val setting = GuiSetting(6, "Nested Page 2")
                addPage(2, setting) {
                    val item2 = ItemStack(Material.DIAMOND).toGuiItem()
                    item2.name = "Go back to Nested Page 1"
                    addItem(item2) {
                        openPage(it.whoClicked as Player, 1)
                    }
                    val item3 = ItemStack(Material.GOLD_INGOT).toGuiItem()
                    item3.name = "Go to Nested Page 3"
                    addItem(item3) {
                        openPage(it.whoClicked as Player, 3)
                    }
                    val setting2 = GuiSetting(6, "Nested Page 2")

                    addPage(3, setting2) {
                        val item4 = ItemStack(Material.IRON_INGOT).toGuiItem()
                        item4.name = "Go back to Nested Page 2"
                        addItem(item4) {
                            openPage(it.whoClicked as Player, 2)
                        }
                    }
                }

            }

        }.open(player)
        ChestGUI(6, "Nested GUI").open(player)

    }

}





