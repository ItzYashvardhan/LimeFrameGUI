package net.justlime.limeframegui.impl

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.integration.FoliaLibHook
import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.entity.Player
import java.util.Collections

class AnvilEventImpl(private val player: Player, private val setting: AnvilGuiSetting, private val builder: AnvilGUIBuilder) {

    fun open() {
        try {
            // Prepare Context & Styles
            val context = setting.style.copy()
            if (context.viewer == null) context.viewer = player

            val rawTitle = FontStyle.applyStyle(setting.title, context, context.stylishTitle)
            val rawLabel = FontStyle.applyStyle(setting.label, context, false)
            val jsonTitle = componentToJson(rawTitle)

            val styledLeft = styleItem(setting.leftItem, context)
            val styledRight = styleItem(setting.rightItem, context)
            val styledOutput = styleItem(setting.outPutItem, context)

            // Play Sound
            val openSound = setting.openSound ?: context.openSound
            openSound.playSound(player)

            // Initialize Anvil Builder
            val anvilBuilder = AnvilGUI.Builder()
                .plugin(LimeFrameAPI.getPlugin())
                .jsonTitle(jsonTitle)
                .text(rawLabel)
                .itemLeft(styledLeft.toItemStack())
                .itemOutput(styledOutput.toItemStack())

            // Feature: Right Item
            if (styledRight.material != org.bukkit.Material.AIR) {
                anvilBuilder.itemRight(styledRight.toItemStack())
            }

            // Feature: Prevent Close
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

            anvilBuilder
                .onClose {
                    builder.onCloseHandler?.invoke(player)
                }
                .onClick { slot, state ->
                    handleClicks(slot, state, rawLabel, context)
                }
                .open(player)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClicks(slot: Int, state: AnvilGUI.StateSnapshot, rawLabel: String, context: net.justlime.limeframegui.models.GuiStyleSheet): List<AnvilGUI.ResponseAction> {

        // Cancellation
        if (slot != AnvilGUI.Slot.OUTPUT) {
            val sound = setting.cancelSound ?: context.closeSound
            sound.playSound(player)
            builder.onCancelHandler?.invoke(player)
            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }

        // Input Logic
        val fullInputText = state.text
        val userInput = if (fullInputText.startsWith(rawLabel, ignoreCase = true) && rawLabel.isNotEmpty()) {
            fullInputText.removePrefix(rawLabel).trim()
        } else {
            fullInputText.trim()
        }

        //  Validation (Empty Input)
        if (userInput.isEmpty()) {
            val sound = setting.cancelSound ?: context.clickSound
            sound.playSound(player)
            builder.onInvalidInputHandler?.invoke(player)

            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }

        // Success
        val sound = setting.submitSound ?: context.clickSound
        sound.playSound(player)
        builder.onConfirmHandler?.invoke(player, userInput)

        return Collections.singletonList(AnvilGUI.ResponseAction.close())
    }

    private fun styleItem(item: GuiItem, context: net.justlime.limeframegui.models.GuiStyleSheet): GuiItem {
        return item.clone().apply {
            name = FontStyle.applyStyle(name, context, style.stylishName)
            lore = FontStyle.applyStyle(lore, context, style.stylishLore)
        }
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