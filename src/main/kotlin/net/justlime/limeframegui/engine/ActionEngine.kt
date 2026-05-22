package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.manager.GuiManager
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.GuiActionPack
import net.justlime.limeframegui.models.registry.GuiSound
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.registry.ButtonRegistry
import net.justlime.limeframegui.registry.component.SoundRegistry
import net.justlime.limeframegui.registry.ActionTagRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

object ActionEngine {

    init {
        ActionTagRegistry.register("[message]") { player, payload, _ ->
            // TODO: Pass through Hex/MiniMessage color translator
            player.sendMessage(payload)
        }

        ActionTagRegistry.register("[sound]") { player, payload, _ ->
            GuiSound.playPack(player, SoundRegistry.get(payload))
        }

        ActionTagRegistry.register("[console]") { player, payload, _ ->
            val cmd = payload.replace("{player}", player.name)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
        }

        ActionTagRegistry.register("[player]") { player, payload, _ ->
            val cmd = payload.replace("{player}", player.name)
            player.performCommand(cmd)
        }

        ActionTagRegistry.register("[actions]") { player, payload, handler ->
            executePack(player, payload, ClickType.UNKNOWN, handler)
        }

        ActionTagRegistry.register("[run]") { player, payload, handler ->
            runCustomJavaCode(player, payload, handler)
        }

        ActionTagRegistry.register("[code]") { player, payload, handler ->
            runCustomJavaCode(player, payload, handler)
        }

        ActionTagRegistry.register("[open_page]") { player, payload, handler ->
            val pageId = payload.toIntOrNull() ?: 0
            handler?.open(player, pageId)
        }

        ActionTagRegistry.register("[open_gui]") { player, payload, _ ->
            GuiManager.open(player, payload)
        }

        ActionTagRegistry.register("[open]") { player, payload, handler ->
            val parts = payload.split(" ")
            val guiId = parts[0]
            val pageId = parts.getOrNull(1)?.toIntOrNull()
            GuiManager.open(player, guiId)
            if (pageId != null) handler?.open(player, pageId)
        }

        ActionTagRegistry.register("[close]") { player, _, _ ->
            player.closeInventory()
        }

        ActionTagRegistry.register("[update]") { _, _, handler ->
            handler?.session?.softRefresh()
        }

        ActionTagRegistry.register("[refresh]") { _, _, handler ->
            handler?.session?.softRefresh()
        }

        ActionTagRegistry.register("[hard_refresh]") { _, _, handler ->
            handler?.session?.refresh()
        }
    }

    /**
     * Executes an action pack.
     * @param handler The active GUI handler (nullable, as actions can run before GUI opens).
     */
    fun executePack(player: Player, actionPackId: String, clickType: ClickType, handler: GuiEventHandler?) {
        val pack = ActionRegistry.get(actionPackId)

        if (pack == null) {
            // Fallback: If it's not in the YAML registry, it might be a hardcoded Java action!
            runCustomJavaCode(player, actionPackId, handler)
            return
        }

        when (pack) {
            is GuiActionPack.Sequence -> {
                // Loop through sequence priorities
                for (node in pack.nodes) {
                    if (ConditionEngine.checkRequirements(player, node.requirements)) {
                        executeStandardNode(player, node, clickType, handler)
                        return // First match wins!
                    }
                }
                // Fallback to 'else' block if none matched
                pack.fallback?.let { executeStandardNode(player, it, clickType, handler) }
            }

            is GuiActionPack.Standard -> {
                if (ConditionEngine.checkRequirements(player, pack.requirements)) {
                    executeStandardNode(player, pack, clickType, handler)
                } else {
                    executeBehavior(player, pack.denyBehavior, handler)
                }
            }
        }
    }

    private fun executeStandardNode(
        player: Player,
        node: GuiActionPack.Standard,
        clickType: ClickType,
        gui: GuiEventHandler?
    ) {
        val clickString = clickType.name.lowercase().replace("_", "-")

        // Priority: 1. Exact Click (e.g., shift-left-click) -> 2. Base (click) -> 3. Do nothing
        val behavior = node.clickActions["$clickString-click"]
            ?: node.clickActions[clickString]
            ?: (if (clickString.contains("shift")) node.clickActions["shift-click"] else null)
            ?: node.clickActions["click"]
            ?: return

        executeBehavior(player, behavior, gui)
    }

    /**
     * Resolves Switch-Case (When) logic and PlaceholderAPI math.
     */
    fun executeBehavior(player: Player, behavior: ActionBehavior, handler: GuiEventHandler?) {
        when (behavior) {
            is ActionBehavior.Simple -> runActions(player, behavior.actions, handler)

            is ActionBehavior.When -> {
                // 1. Resolve target value via PAPI
                val rawPlaceholder = behavior.valuePlaceholder
                var resolvedValue = rawPlaceholder.replace("{", "%").replace("}", "%")

                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    resolvedValue = PlaceholderAPI.setPlaceholders(player, resolvedValue)
                }
                if (resolvedValue.isNullOrEmpty()) resolvedValue = "false"
                resolvedValue = resolvedValue.removeSuffix(".0") // Clean Vault decimals

                // 2. Find exact match, fallback to "else" or "default"
                val matchedBehavior = behavior.results[resolvedValue]
                    ?: behavior.results["else"]
                    ?: behavior.results["default"]

                if (matchedBehavior != null) {
                    executeBehavior(player, matchedBehavior, handler)
                }
            }
        }
    }

    private fun runActions(player: Player, actions: List<String>, handler: GuiEventHandler?) {
        val registeredTags = ActionTagRegistry.getTags()

        for (action in actions) {
            val str = action.trim()
            val lowerStr = str.lowercase()
            var matched = false

            for ((tag, executor) in registeredTags) {
                if (lowerStr.startsWith(tag)) {
                    // Extract payload safely, keeping original casing (e.g. "Hello World" from "[message] Hello World")
                    val payload = str.substring(tag.length).trim()

                    // Execute the matched tag logic
                    executor.execute(player, payload, handler)
                    matched = true
                    break // Stop checking other tags for this line
                }
            }

            if (!matched) {
                Bukkit.getLogger().warning("[LimeFrameGUI] Unknown action tag used: '$str'")
            }
        }
    }

    private fun runCustomJavaCode(player: Player, identifier: String, gui: GuiEventHandler?) {
        if (gui == null) {
            println("[LimeFrameGUI] Warning: Attempted to run Java code '$identifier' outside of a GUI context.")
            return
        }

        val success = ButtonRegistry.execute(identifier, player, gui)
        if (!success) {
            println("[LimeFrameGUI] Warning: Button clicked with unknown action ID or unregistered Java Code: '$identifier'")
        }
    }
}