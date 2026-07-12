package net.justlime.limeframegui.event

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.builder.AnvilGuiBuilder
import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.engine.ActionEngine
import net.justlime.limeframegui.engine.TextResolver
import net.justlime.limeframegui.integration.FoliaLibHook
import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.models.registry.GuiSound
import net.justlime.limeframegui.models.response.ActionTagRegistryResponse
import net.justlime.limeframegui.registry.component.SoundRegistry
import net.justlime.limeframegui.session.ItemRenderer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import java.util.*

class AnvilEventImpl(
    private val player: Player,
    private val setting: AnvilGuiSetting,
    private val builder: AnvilGuiBuilder
) {

    fun open() {
        try {
            // Prepare Context & Styles
            val context = setting.style.copy()
            if (context.viewer == null) context.viewer = player

            val resolvedTitle = TextResolver.resolve(player, setting.title, setting)
            val rawTitle = FontStyle.applyStyle(resolvedTitle, context, context.textSettings.title)

            val leftStack = ItemRenderer.render(setting.leftItem, context, setting)
            val outputStack = ItemRenderer.render(setting.outPutItem, context, setting)

            val resolvedLabel = TextResolver.resolve(player, setting.label, setting)
            val rawLabel = FontStyle.applyStyle(resolvedLabel, context, TextFormatRule())

            val jsonTitle = componentToJson(rawTitle)

            setting.openSoundString?.let { alias ->
                GuiSound.playPack(player, SoundRegistry.get(alias))
            }

            // Anvil Builder
            val anvilBuilder = AnvilGUI.Builder()
                .plugin(LimeFrameAPI.getPlugin())
                .jsonTitle(jsonTitle)
                .text(rawLabel)
                .itemLeft(leftStack)
                .itemOutput(outputStack)

            // Right Item (Checked against baseItem.type)
            if (setting.rightItem.baseItem.type != Material.AIR) {
                val rightStack = ItemRenderer.render(setting.rightItem, context, setting)
                anvilBuilder.itemRight(rightStack)
            }

            // Prevent Close
            if (setting.preventClose) {
                anvilBuilder.preventClose()
            }

            if (FoliaLibHook.isInitialized()) {
                anvilBuilder.mainThreadExecutor { command ->
                    FoliaLibHook.foliaLib.scheduler.runNextTick {
                        command.run()
                    }
                }
            }

            anvilBuilder.onClose { builder.onCloseHandler?.invoke(player) }
            anvilBuilder.onClick { slot, state ->
                handleClicks(slot, state, rawLabel, context)
            }
            anvilBuilder.open(player)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClicks(
        slot: Int,
        state: AnvilGUI.StateSnapshot,
        rawLabel: String,
        context: GuiStyleSheet
    ): List<AnvilGUI.ResponseAction> {


        if (slot == AnvilGUI.Slot.INPUT_LEFT || slot == AnvilGUI.Slot.INPUT_RIGHT) {
            val isLeft = slot == AnvilGUI.Slot.INPUT_LEFT
            val itemAction = if (isLeft) setting.leftItem.style.action else setting.rightItem.style.action
            val callback = if (isLeft) builder.onLeftClickHandler else builder.onRightClickHandler
            if (!itemAction.isNullOrEmpty()) {
                val response = ActionTagRegistryResponse(player, "", null, setting)
                ActionEngine.executePack(response, itemAction, ClickType.LEFT)
            }
            callback?.invoke(state)
            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }

        val fullInputText = state.text
        var plainLabel = MiniMessage.miniMessage().stripTags(rawLabel)
        plainLabel = plainLabel.replace(Regex("(?i)§[0-9a-fk-orx]"), "")
        plainLabel = plainLabel.trim()
        val userInput = if (builder.keepLabel) {
            fullInputText
        } else if (plainLabel.isNotEmpty() && fullInputText.startsWith(plainLabel, ignoreCase = true)) {
            fullInputText.removePrefix(plainLabel).trim()
        } else {
            fullInputText.replace(plainLabel, "", true).trim()
        }

        if (userInput.isEmpty()) {
            builder.onInvalidInputHandler?.invoke(player)
            return Collections.emptyList()
        }

        if (slot == AnvilGUI.Slot.OUTPUT) {
            val outputAction = setting.outPutItem.style.action
            if (!outputAction.isNullOrEmpty()) {
                val injectedSetting = setting.clone()
                injectedSetting.localPlaceholders = setting.localPlaceholders + mapOf("input" to userInput)

                val response = ActionTagRegistryResponse(player, "", null, injectedSetting)
                ActionEngine.executePack(response, outputAction, ClickType.LEFT)
            }

            builder.onOutputClickHandler?.invoke(state, userInput)
            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }
        return Collections.singletonList(AnvilGUI.ResponseAction.close())
    }

    private fun componentToJson(text: String): String {
        val component = if (text.contains("§")) {
            LegacyComponentSerializer.legacySection().deserialize(text)
        } else {
            try {
                MiniMessage.miniMessage().deserialize(text)
            } catch (_: Exception) {
                Component.text(text)
            }
        }
        return GsonComponentSerializer.gson().serialize(component)
    }
}