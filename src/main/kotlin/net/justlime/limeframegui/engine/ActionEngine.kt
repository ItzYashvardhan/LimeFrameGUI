package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.manager.GuiManager
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.GuiActionPack
import net.justlime.limeframegui.models.registry.GuiSound
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.registry.component.ButtonRegistry
import net.justlime.limeframegui.registry.component.SoundRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

object ActionEngine {

    /**
     * Executes an action pack.
     * @param gui The active GUI handler (used for [close] and [open] tags).
     */
    fun executePack(player: Player, actionPackId: String, clickType: ClickType, gui: GuiEventHandler) {
        val pack = ActionRegistry.get(actionPackId)

        if (pack == null) {
            // Fallback: If it's not in the YAML registry, it might be a hardcoded Java action!
            runCustomJavaCode(player, actionPackId, gui)
            return
        }

        when (pack) {
            is GuiActionPack.Sequence -> {
                // Loop through sequence priorities
                for (node in pack.nodes) {
                    if (ConditionEngine.checkRequirements(player, node.requirements)) {
                        executeStandardNode(player, node, clickType, gui)
                        return // First match wins!
                    }
                }
                // Fallback to 'else' block if none matched
                pack.fallback?.let { executeStandardNode(player, it, clickType, gui) }
            }

            is GuiActionPack.Standard -> {
                if (ConditionEngine.checkRequirements(player, pack.requirements)) {
                    executeStandardNode(player, pack, clickType, gui)
                } else {
                    executeBehavior(player, pack.denyBehavior, gui)
                }
            }
        }
    }

    private fun executeStandardNode(
        player: Player,
        node: GuiActionPack.Standard,
        clickType: ClickType,
        gui: GuiEventHandler
    ) {
        val clickString = clickType.name.lowercase().replace("_", "-")

        // Priority: 1. Exact Click (e.g., shift-left-click) -> 2. Base (click) -> 3. Do nothing
        val behavior = node.clickActions["$clickString-click"]
            ?: node.clickActions[clickString]
            ?: node.clickActions["click"]
            ?: return

        executeBehavior(player, behavior, gui)
    }

    /**
     * Resolves Switch-Case (When) logic and PlaceholderAPI math.
     */
    private fun executeBehavior(player: Player, behavior: ActionBehavior, gui: GuiEventHandler) {
        when (behavior) {
            is ActionBehavior.Simple -> runActions(player, behavior.actions, gui)

            is ActionBehavior.When -> {
                // 1. Resolve target value via PAPI
                var resolvedValue = behavior.valuePlaceholder.replace("{", "%").replace("}", "%")
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    resolvedValue = PlaceholderAPI.setPlaceholders(player, resolvedValue)
                }
                resolvedValue = resolvedValue.removeSuffix(".0") // Clean Vault decimals

                // 2. Find exact match, fallback to "else" or "default"
                val matchedBehavior = behavior.results[resolvedValue]
                    ?: behavior.results["else"]
                    ?: behavior.results["default"]

                if (matchedBehavior != null) {
                    executeBehavior(player, matchedBehavior, gui)
                }
            }
        }
    }

    /**
     * The actual tag parser that runs the Bukkit actions.
     */
    private fun runActions(player: Player, actions: List<String>, gui: GuiEventHandler) {
        for (action in actions) {
            val str = action.trim()
            when {
                str.startsWith("[message]", true) -> {
                    // TODO: Pass through your Hex/MiniMessage color translator
                    player.sendMessage(str.removePrefix("[message]").trim())
                }

                str.startsWith("[sound]", true) -> {
                    val alias = str.removePrefix("[sound]").trim()
                    GuiSound.playPack(player, SoundRegistry.get(alias))
                }

                str.startsWith("[console]", true) -> {
                    val cmd = str.removePrefix("[console]").trim().replace("%player_name%", player.name)
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
                }

                str.startsWith("[player]", true) -> {
                    val cmd = str.removePrefix("[player]").trim().replace("%player_name%", player.name)
                    player.performCommand(cmd)
                }

                str.startsWith("[actions]", true) -> {
                    val nextPack = str.removePrefix("[actions]").trim()
                    executePack(player, nextPack, ClickType.UNKNOWN, gui)
                }

                str.startsWith("[run]", true) || str.startsWith("[code]", true) -> {
                    val codeId = str.replace("[run]", "", true).replace("[code]", "", true).trim()
                    runCustomJavaCode(player, codeId, gui)
                }

                str.startsWith("[open_page]", true) -> {
                    val pageStr = str.removePrefix("[open]").trim()
                    val pageId = pageStr.toIntOrNull() ?: 0
                    gui.open(player, pageId)
                }

                str.startsWith("[open_gui]", true) -> {
                    val guiId = str.removePrefix("[open_gui]").trim()
                    GuiManager.open(player, guiId)
                }

                str.startsWith("[open]",true) ->{
                    val parts = str.removePrefix("[open]").trim().split(" ")
                    val guiId = parts[0]
                    val pageId = parts.getOrNull(1)?.toIntOrNull()
                    GuiManager.open(player, guiId)
                    if (pageId != null) gui.open(player, pageId)
                }

                str.startsWith("[close]", true) -> {
                    player.closeInventory()
                }
                str.equals("[update]", true) || str.equals("[refresh]", true) -> {
                    gui.session.softRefresh()
                }

                str.equals("[hard_refresh]", true) -> {
                    gui.session.refresh()
                }

            }
        }
    }

    private fun runCustomJavaCode(player: Player, identifier: String, gui: GuiEventHandler) {
        val success = ButtonRegistry.execute(identifier, player, gui)
        if (!success) {
            println("[LimeFrameGUI] Warning: Button clicked with unknown action ID or unregistered Java Code: '$identifier'")
        }
    }
}