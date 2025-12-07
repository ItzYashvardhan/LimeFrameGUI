package net.justlime.limeframegui.example.commands

import net.justlime.limeframegui.handle.CommandHandler
import net.justlime.limeframegui.impl.ConfigHandler
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.type.ChestGUI
import net.justlime.limeframegui.utilities.item
import net.justlime.limeframegui.utilities.toGuiItem
import net.justlime.limeframegui.utilities.update
import org.bukkit.Bukkit
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

            "save" -> savePage(sender)
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
                formattedPage(Example.setting.copy(), sender)
            }

            "formatted2" -> {
                GuiManager.openFormattedGUI(sender)
            }

            else -> {}
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String?>
    ): List<String?> {
        val completion = mutableListOf<String>()
        if (args.isNotEmpty()) completion.addAll(listOf("save", "page", "home", "nested", "formatted", "formatted2"))
        return completion
    }

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
            }

            //Global Click handler
            onClick { it.isCancelled = true }

            //This item added to every page
            //You can used it as Custom Background Design
            item4.slot = 5
            setItem(item4) {
                it.whoClicked.sendMessage("You click on global item")
            }

//            addPage(id = 2, title = "Kebab Mai Hadi"){
//                item4.slotList = (11..20).toList()
//                setItem(item4){
//                    it.whoClicked.sendMessage("You click on global item")
//                }
//            }
            addPage(GUISetting(6, "Regular Page {page}")) {
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

            setting.title = "Custom Page {page}"
            setting.rows = 3
            addPage {
                addItem(item3) {
                    it.whoClicked.sendMessage("Clicked on Item 5 at page $currentPage")
                }
                addItem(item3) {
                    it.whoClicked.sendMessage("Clicked on Item 6")
                }
            }

            addPage(GUISetting(4, "Custom Page {page}")) {
                addItem(item4) {
                    it.whoClicked.sendMessage("Clicked on Item ${it.slot} at page $currentPage")
                }
                addItem(item3) {
                    it.whoClicked.sendMessage("Clicked on Item ${it.slot} at page $currentPage")
                }
            }

        }.open(player)
        ChestGUI(6, "Pager GUI").open(player)
    }

    fun simpleGUI(): ChestGUI {
        return ChestGUI(6, "Simple GUI") {
            onClick { it.isCancelled = true }

            val item = ItemStack(Material.DIAMOND).toGuiItem().apply {
                name = "§aClick Me!"
                lore = mutableListOf("§7This is a simple item.")
            }

            addItem(item) {
                it.whoClicked.sendMessage("§aYou clicked the diamond!")
            }
        }
    }

    fun homePage(player: Player) {

        ChestGUI(1, "hello %player_name%") {

            var value = 0

            onClose {
                it.player.sendMessage("Closing Inventory")
            }

            onClick { it.isCancelled = true }

            val simpleItem = ItemStack(Material.GRASS_BLOCK).toGuiItem().apply { name = "Open Simple GUI for %player_name%" }

            addItem(simpleItem) {
                simpleGUI().open(it.whoClicked as Player)
            }

            val pageItem = ItemStack(Material.BOOK).toGuiItem().apply { name = "Open Pager GUI"; }

            val staticExtraItem = GuiItem(Material.PAPER, name = "Entered $value", lore = listOf("§aPlayTime: %statistic_time_played%"))
            val dynamicExtraItem = GuiItem(Material.PAPER, nameState = { "Entered $value" }, loreState = { listOf("§aPlayTime: %statistic_time_played%") })
            addItem(pageItem) {
                pageExample(player)
            }

            val items = mutableListOf(staticExtraItem, dynamicExtraItem).toList()

            addItem(items) { event ->
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

    fun savePage(player: Player) {

        val config = ConfigHandler("config.yml")
        val setting = config.loadInventorySetting("inventory")
        val inventory = config.loadInventory("inventory") ?: Bukkit.createInventory(null, setting.rows * 9, setting.title)

        ChestGUI(setting.rows, setting.title) {

            onOpen {}
            loadInventoryContents(inventory)

            onClose {
                val inventory = pages[0]?.inventory ?: return@onClose //Definitely not happening
                config.saveInventory("inventory", inventory, setting.title)
            }
        }.open(player)
        ChestGUI(setting.rows, setting.title).open(player)

    }

    fun nestedPage(player: Player) {

        //Useful if you gui can have various different page (Great for Tree Like Structure)
        //Don't use nav{} //It will give unexpected behaviour

        ChestGUI(6, "Nested GUI") {
            onClick { it.isCancelled = true }

            addPage(GUISetting(6, "Nested Page 1")) {
                val item1 = ItemStack(Material.PAPER).toGuiItem()
                item1.name = "Go to Nested Page 2"
                addItem(item1) {
                    openPage(it.whoClicked as Player, 2)
                }

                val setting = GUISetting(6, "Nested Page 2")
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
                    val setting2 = GUISetting(6, "Nested Page 2")

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

private object Example {
    var value = true
    val setting = GUISetting(6, "Example %betterteams_name%").apply {
        this.styleSheet?.apply {
            stylishName = true
            stylishLore = true
            stylishTitle = true
        }
    }

}

private object GuiManager {
    fun openFormattedGUI(player: Player) {
        formattedPage(Example.setting, player)
    }
}

private fun formattedPage(setting: GUISetting, player: Player) {

    setting.styleSheet?.placeholder = mapOf("{world}" to player.world.name + " at " + player.location.x.toInt() + player.location.y.toInt() + player.location.z.toInt())
    ChestGUI(setting) {

        val item3 = GuiItem(
            material = Material.PLAYER_HEAD, name = "Player: %player_name%", lore = listOf(
                "<green>Playtime Stats1: %statistic_time_played%", "<green>World: <white>{world}</white>", "<aqua>Click to refresh"
            ), texture = "%player_name%"

        )

        val item31 = GuiItem(
            material = Material.PLAYER_HEAD, name = "Player: %player_name%", lore = listOf(
                "<green>Playtime Stats2: %statistic_time_played%", "<aqua>Click to refresh"
            ), texture = "%player_name%"

        )

        val finalItem = if (Example.value) item3 else item31
        Example.value = !Example.value

        val item1 = GuiItem(
            Material.PAPER, name = "<gradient:red:blue>This is a Gradient title</gradient>", lore = listOf(
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
                Material.PAPER, name = "<gradient:red:blue>This is a Gradient title</gradient>", lore = listOf(
                    "<red>This is a red line</red>", "<green>This is a green line</green>", "<blue>This is a blue line</blue>"
                )
            )

            addItem(item12) {
                it.whoClicked.sendMessage("Clicked formatted item!")
            }

            val item2 = GuiItem(
                material = Material.GOLD_INGOT, name = "Player: %betterteams_name%", lore = listOf(
                    "<gold>Balance: %vault_eco_balance%", "<white>Location: %player_x%, %player_y%, %player_z%"
                )
            )

            val item4 = GuiItem(
                material = Material.TOTEM_OF_UNDYING, name = "<#FF00FF>Custom PlaceHolder</#FF00FF>", lore = listOf(
                    "<gray>World: {world}</gray>", "<gray>Location: {location}</gray>"

                ), styleSheet = setting.styleSheet?.copy(
                    placeholder = mutableMapOf(
                        "{world}" to player.world.name, "{location}" to "${player.location.x.toInt()}, ${player.location.y.toInt()}, ${player.location.z.toInt()}"
                    )
                )
            )

            val item5 = GuiItem(Material.PLAYER_HEAD, texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQzMDhhZTI3YjU4YjY5NjQ1NDk3ZjlkYTg2NTk3ZWRhOTQ3ZWFjZDEwYzI5ZTNkNGJiZjNiYzc2Y2ViMWVhYiJ9fX0=")



            addItem(item2) {
                it.whoClicked.sendMessage("Clicked player placeholder item!")
            }

            addItem(finalItem) { event ->
                event.item = if (Example.value) item3 else item31
                event.item = if (Example.value) item3 else item31
                event.update()
            }

            addItem(item4)
            addItem(item5)
        }
    }.open(player,1)

}


