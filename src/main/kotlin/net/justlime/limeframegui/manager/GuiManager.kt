package net.justlime.limeframegui.manager

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.builder.AnvilGuiBuilder
import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.engine.ActionEngine
import net.justlime.limeframegui.engine.ConditionEngine
import net.justlime.limeframegui.engine.TextResolver
import net.justlime.limeframegui.event.AnvilEventImpl
import net.justlime.limeframegui.integration.FoliaLibHook
import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.models.GuiPageTemplate
import net.justlime.limeframegui.models.GuiState
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.response.ActionTagRegistryResponse
import net.justlime.limeframegui.models.response.ListPopulatorResponse
import net.justlime.limeframegui.registry.gui.ListPopulatorRegistry
import net.justlime.limeframegui.registry.gui.PageRegistry
import net.justlime.limeframegui.registry.input.InputRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.collections.ArrayDeque

/**
 * The central rendering engine for the GUI framework.
 * Responsible for taking compiled, static blueprints (GuiPageTemplate) from the registry,
 * applying the viewer's context (placeholders, themes, permissions), and assembling
 * the interactive inventory safely.
 */
object GuiManager {

    private val playerHistory = mutableMapOf<UUID, ArrayDeque<GuiState>>()
    private val currentGui = mutableMapOf<UUID, GuiState>()

    /**
     * Opens an Anvil Input GUI.
     * @param player The player opening the GUI.
     * @param inputId The YAML file ID from the gui/inputs/ folder.
     * @param commandRaw The raw payload from the Action tag (e.g., "team {input}").
     * @param context The Context (IContextSetting) of the parent Chest GUI they clicked from.
     */
    fun openInput(player: Player, inputId: String, commandRaw: String, context: IContextSetting): Boolean {
        val setting = InputRegistry.get(inputId)?.clone() ?: run {
            println("[LimeFrameGUI] Error: Attempted to open unknown input '$inputId'")
            return false
        }

        setting.localVariables = context.localVariables + setting.localVariables
        setting.localPlaceholders = context.localPlaceholders + setting.localPlaceholders

        val resolvedReqs = TextResolver.resolveList(player, setting.openRequirements, setting)
        if (!ConditionEngine.checkRequirements(player, resolvedReqs)) {
            val response = ActionTagRegistryResponse(player, commandRaw, null, setting)
            ActionEngine.executeBehavior(response, setting.denyBehavior)
            return false
        }

        val currentlyOpen = currentGui[player.uniqueId]
        if (currentlyOpen != null && !currentlyOpen.id.startsWith("input:")) {
            playerHistory.getOrPut(player.uniqueId) { ArrayDeque() }.addLast(currentlyOpen)
        }
        currentGui[player.uniqueId] = GuiState("input:$inputId", context.style.offlinePlayer)

        val builder = AnvilGuiBuilder(setting)

        builder.onConfirmClick { state, userInput ->
            if (userInput.isNotBlank()) {
                val finalCommand = commandRaw.replace("{input}", userInput)
                val response = ActionTagRegistryResponse(player, finalCommand, null, context)
                val behavior = ActionBehavior.Simple(listOf(finalCommand))
                ActionEngine.executeBehavior(response, behavior)
            }
        }

        builder.onClose { closedPlayer ->

            if (currentGui[closedPlayer.uniqueId]?.id == "input:$inputId") {
                if (FoliaLibHook.isInitialized()) {
                    FoliaLibHook.foliaLib.scheduler.runNextTick { back(closedPlayer) }
                } else {
                    Bukkit.getScheduler().runTaskLater(LimeFrameAPI.getPlugin(), Runnable {
                        back(closedPlayer)
                    }, 1L)
                }
            }
        }

        AnvilEventImpl(player, setting, builder).open()
        return true
    }

    fun open(player: Player, guiId: String, recordHistory: Boolean = true, targetData: OfflinePlayer? = null): Boolean {
        val template: GuiPageTemplate = PageRegistry.get(guiId) ?: run {
            println("[LimeFrameGUI] Error: Attempted to open unknown page '$guiId'")
            return false
        }

        val resolvedReqs = TextResolver.resolveList(player, template.setting.openRequirements, template.setting)
        if (!ConditionEngine.checkRequirements(player, resolvedReqs)) {
            val response = ActionTagRegistryResponse(player, "", null, template.setting)
            ActionEngine.executeBehavior(response, template.setting.denyBehavior)
            return false
        }
        val updatedStyle = template.setting.style.copy(
            offlinePlayer = targetData ?: template.setting.style.offlinePlayer ?: template.setting.style.viewer
        )
        if (recordHistory) {
            val currentlyOpen = currentGui[player.uniqueId]
            if (currentlyOpen != null && currentlyOpen.id != guiId) {
                playerHistory.getOrPut(player.uniqueId) { ArrayDeque() }.addLast(currentlyOpen)
            }
        }
        currentGui[player.uniqueId] = GuiState(guiId, updatedStyle.offlinePlayer)

        val resolvedTitle = TextResolver.resolve(player, template.setting.title, template.setting, updatedStyle)

        val localizedSetting = template.setting.copy(
            title = resolvedTitle, style = updatedStyle
        )

        ChestGUI(localizedSetting) {
            onClick { it.isCancelled = true }

            val activeItems = template.permissionItems.entries
                .firstOrNull { (perm, _) -> perm == "default" || player.hasPermission(perm) }
                ?.value ?: template.permissionItems["default"] ?: emptyList()

            val nextBtn = activeItems.find { it.style.action == "core_next_page" }
            val prevBtn = activeItems.find { it.style.action == "core_prev_page" }

            template.dynamicMask?.let { mask ->
                nav {
                    nextBtn?.let {
                        nextSlot = it.slot ?: 0
                        nextItem = it.clone().apply { style.viewer = player }
                    }
                    prevBtn?.let {
                        prevSlot = it.slot ?: 0
                        prevItem = it.clone().apply { style.viewer = player }
                    }
                    buffer {
                        renderLimit = mask.buffer.renderLimit
                        margin = mask.buffer.margin
                        cleanupMargin = mask.buffer.cleanupMargin
                    }
                }
            }

            activeItems.forEach { templateItem ->
                if (templateItem.style.action == "core_next_page" || templateItem.style.action == "core_prev_page") return@forEach
                if (templateItem.baseItem.type == Material.AIR) return@forEach

                if (templateItem.stateId != null && templateItem.states.isNotEmpty()) {
                    templateItem.states.forEach { (stateKey, stateOverride) ->
                        val stateItem = stateOverride.clone()
                        stateItem.slot = templateItem.slot
                        stateItem.style.action = templateItem.style.action
                        stateItem.style.viewer = player

                        val condition = "[condition] '{var:${templateItem.stateId}}' == '$stateKey'"
                        val mergedReqs = stateItem.viewRequirements.toMutableList()
                        mergedReqs.add(condition)
                        stateItem.viewRequirements = mergedReqs

                        if (stateItem.updateInterval == null) {
                            stateItem.updateInterval = templateItem.updateInterval
                        }

                        setItem(stateItem) { event -> stateItem.onClick(event) }
                    }
                } else {
                    // Normal Static Item
                    val playerItem = templateItem.clone()
                    playerItem.style.offlinePlayer = localizedSetting.style.offlinePlayer
                    if (playerItem.style.offlinePlayer == null) {
                        playerItem.style.viewer = player
                    }
                    setItem(playerItem) { event -> playerItem.onClick(event) }
                }
            }

            template.dynamicMask?.let { mask ->
                val response = ListPopulatorResponse(player, mask, localizedSetting)
                val populatedItems = ListPopulatorRegistry.getItems(mask.populatorId, response)
                addPage {
                    populatedItems.forEach { item ->
                        addItem(item) { event -> item.onClick(event) }
                    }
                }
            }
        }.open(player)
        return true
    }

    /**
     * Navigates the player back to their previous GUI.
     */
    fun back(player: Player): Boolean {
        val history = playerHistory[player.uniqueId] ?: return false
        val previousState = history.removeLastOrNull() ?: return false
        return open(player, previousState.id, recordHistory = false, targetData = previousState.targetData)
    }

    /**
     * Clears a player's history (Call this on PlayerQuitEvent or when they close the menu entirely)
     */
    fun clearHistory(uniqueId: UUID) {
        playerHistory.remove(uniqueId)
        currentGui.remove(uniqueId)
    }
}