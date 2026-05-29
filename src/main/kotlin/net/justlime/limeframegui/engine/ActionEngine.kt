package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.event.GuiEventHandler
import net.justlime.limeframegui.manager.GuiManager
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.GuiActionPack
import net.justlime.limeframegui.models.registry.GuiSound
import net.justlime.limeframegui.models.response.ActionTagRegistryResponse
import net.justlime.limeframegui.registry.ButtonRegistry
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.registry.component.ActionTagRegistry
import net.justlime.limeframegui.registry.component.SoundRegistry
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

object ActionEngine {

    init {
        ActionTagRegistry.register("[message]") { response ->
            val text = response.context?.let {
                TextResolver.resolve(response.player, response.payload, it)
            } ?: response.payload
            val coloredText =
                FontStyle.miniMessage?.legacyToMini(text) ?: ChatColor.translateAlternateColorCodes('&', text)
            response.player.sendMessage(coloredText)
        }

        ActionTagRegistry.register("[sound]") { response ->
            GuiSound.playPack(response.player, SoundRegistry.get(response.payload))
        }

        ActionTagRegistry.register("[console]") { response ->
            var cmd = response.context?.let {
                TextResolver.resolve(response.player, response.payload, it)
            } ?: response.payload

            cmd = cmd.replace("{player}", response.player.name)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
        }

        ActionTagRegistry.register("[player]") { response ->
            var cmd = response.context?.let {
                TextResolver.resolve(response.player, response.payload, it)
            } ?: response.payload

            cmd = cmd.replace("{player}", response.player.name)
            response.player.performCommand(cmd)
        }

        ActionTagRegistry.register("[action]") { response ->
            executePack(response, response.payload, ClickType.UNKNOWN)
        }

        // The Anvil Input Tag
        val inputPayloadRegex = Regex("(?i)^(['\"]?[-a-zA-Z0-9_./]+['\"]?)(?:\\s+(.+))?$")
        ActionTagRegistry.register("[input]") { response ->
            if (response.context == null) {
                Bukkit.getLogger().warning("[LimeFrameGUI] Cannot open [input] without a GUI Context!")
                return@register
            }

            val match = inputPayloadRegex.matchEntire(response.payload.trim())
            if (match != null) {
                val inputId = match.groupValues[1].replace("'", "").replace("\"", "")
                val rawCommand = match.groupValues[2]
                response.player.closeInventory()
                GuiManager.openInput(response.player, inputId, rawCommand, response.context)
            } else {
                Bukkit.getLogger().warning("[LimeFrameGUI] Invalid [input] syntax: '${response.payload}'")
            }
        }

        ActionTagRegistry.register("[run]") { response ->
            runCustomJavaCode(response.player, response.payload, response.handler)
        }

        ActionTagRegistry.register("[code]") { response ->
            runCustomJavaCode(response.player, response.payload, response.handler)
        }

        ActionTagRegistry.register("[open_page]") { response ->
            val pageId = response.payload.toIntOrNull() ?: 0
            response.handler?.open(response.player, pageId)
        }

        ActionTagRegistry.register("[open_gui]") { response ->
            GuiManager.open(response.player, response.payload)
        }

        ActionTagRegistry.register("[open]") { response ->
            val parts = response.payload.split(" ")
            val guiId = parts[0]
            val pageId = parts.getOrNull(1)?.toIntOrNull()
            val clickedItem = response.item
            val targetData = clickedItem?.style?.offlinePlayer ?: response.context?.style?.offlinePlayer

            GuiManager.open(response.player, guiId, targetData = targetData)
            if (pageId != null) {
                response.handler?.open(response.player, pageId)
            }
        }

        ActionTagRegistry.register("[close]") { response ->
            response.player.closeInventory()
        }

        ActionTagRegistry.register("[update]") { response ->
            response.handler?.session?.softRefresh()
        }

        ActionTagRegistry.register("[refresh]") { response ->
            response.handler?.session?.softRefresh()
        }

        ActionTagRegistry.register("[hard_refresh]") { response ->
            response.handler?.session?.refresh()
        }

        ActionTagRegistry.register("[back]") { response ->
            val success = GuiManager.back(response.player)
            if (!success) {
                response.player.closeInventory()
            }
        }
    }

    /**
     * Executes an action pack.
     * @param handler The active GUI handler (nullable, as actions can run before GUI opens).
     */
    fun executePack(response: ActionTagRegistryResponse, actionPackId: String, clickType: ClickType) {
        val pack = ActionRegistry.get(actionPackId)

        if (pack == null) {
            runCustomJavaCode(response.player, actionPackId, response.handler)
            return
        }

        when (pack) {
            is GuiActionPack.Sequence -> {
                for (node in pack.nodes) {
                    val resolvedReqs =
                        response.context?.let { TextResolver.resolveList(response.player, node.requirements, it) }
                            ?: node.requirements

                    if (ConditionEngine.checkRequirements(response.player, resolvedReqs)) {
                        executeStandardNode(response, node, clickType)
                        return // First match wins!
                    }
                }
                // Fallback to 'else' block if none matched
                pack.fallback?.let { executeStandardNode(response, it, clickType) }
            }

            is GuiActionPack.Standard -> {
                val resolvedReqs =
                    response.context?.let { TextResolver.resolveList(response.player, pack.requirements, it) }
                        ?: pack.requirements

                if (ConditionEngine.checkRequirements(response.player, resolvedReqs)) {
                    executeStandardNode(response, pack, clickType)
                } else {
                    executeBehavior(response, pack.denyBehavior)
                }
            }
        }
    }

    private fun executeStandardNode(
        response: ActionTagRegistryResponse,
        node: GuiActionPack.Standard,
        clickType: ClickType
    ) {
        val clickString = clickType.name.lowercase().replace("_", "-")

        // Priority: 1. Exact Click (e.g., shift-left-click) -> 2. Base (click) -> 3. Do nothing
        val behavior = node.clickActions["$clickString-click"]
            ?: node.clickActions[clickString]
            ?: (if (clickString.contains("shift")) node.clickActions["shift-click"] else null)
            ?: node.clickActions["click"]
            ?: return

        executeBehavior(response, behavior)
    }

    /**
     * Resolves Switch-Case (When) logic and PlaceholderAPI math.
     */
    fun executeBehavior(response: ActionTagRegistryResponse, behavior: ActionBehavior) {
        when (behavior) {
            is ActionBehavior.Simple -> runActions(response, behavior.actions)

            is ActionBehavior.When -> {
                // 1. Resolve target value using the TextResolver pipeline
                val rawPlaceholder = behavior.valuePlaceholder
                var resolvedValue = if (response.context != null) {
                    TextResolver.resolve(response.player, rawPlaceholder, response.context)
                } else {
                    val fallback = rawPlaceholder.replace("{", "%").replace("}", "%")
                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        PlaceholderAPI.setPlaceholders(response.player, fallback)
                    } else fallback
                }

                if (resolvedValue.isEmpty()) resolvedValue = "false"
                resolvedValue = resolvedValue.removeSuffix(".0") // Clean Vault decimals

                // 2. Find exact match, fallback to "else" or "default"
                val matchedBehavior = behavior.results[resolvedValue]
                    ?: behavior.results["else"]
                    ?: behavior.results["default"]

                if (matchedBehavior != null) {
                    executeBehavior(response, matchedBehavior)
                }
            }
        }
    }

    private fun runActions(response: ActionTagRegistryResponse, actions: List<String>) {
        val registeredTags = ActionTagRegistry.getTags()

        for (i in actions.indices) {
            val str = actions[i].trim()
            if (str.isEmpty()) continue

            val lowerStr = str.lowercase()
            if (lowerStr.startsWith("[delay]")) {
                val ticks = str.substring(7).trim().toLongOrNull() ?: 20L
                val remainingActions = actions.subList(i + 1, actions.size)

                Bukkit.getScheduler().runTaskLater(LimeFrameAPI.getPlugin(), Runnable {
                    runActions(response, remainingActions)
                }, ticks)

                return
            }

            // Normal Tag Execution
            var matched = false
            for ((tag, executor) in registeredTags) {
                if (lowerStr.startsWith(tag)) {
                    val payload = str.substring(tag.length).trim()
                    val tagResponse = response.copy(payload = payload)
                    executor.execute(tagResponse)
                    matched = true
                    break
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