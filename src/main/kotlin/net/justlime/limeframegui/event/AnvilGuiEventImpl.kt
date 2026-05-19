package net.justlime.limeframegui.event

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.builder.AnvilGuiBuilder
import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.integration.FoliaLibHook
import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.registry.GuiSound
import net.justlime.limeframegui.registry.component.SoundRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

class AnvilGuiEventImpl(
    private val player: Player,
    private val setting: AnvilGuiSetting,
    private val builder: AnvilGuiBuilder
) {

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

            // --- INJECTED: Play Open Sound via Registry ---
            setting.openSoundAlias?.let { alias ->
                GuiSound.playPack(player, SoundRegistry.get(alias))
            }

            // Initialize Anvil Builder
            val anvilBuilder = AnvilGUI.Builder()
                .plugin(LimeFrameAPI.getPlugin())
                .jsonTitle(jsonTitle)
                .text(rawLabel)
                .itemLeft(styledLeft.toItemStack())
                .itemOutput(styledOutput.toItemStack())

            // Feature: Right Item
            if (styledRight.material != Material.AIR) {
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

        // --- INJECTED: Centralized Cancel/Close Sound Logic ---
        val playCancelSound = {
            val cancelAlias = setting.cancelSoundAlias ?: context.closeSoundAlias
            cancelAlias?.let { alias ->
                GuiSound.playPack(player, SoundRegistry.get(alias))
            }
        }

        if (slot == AnvilGUI.Slot.INPUT_RIGHT) {
            playCancelSound()
            builder.onRightClickHandler?.invoke(state)
            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }

        if (slot == AnvilGUI.Slot.INPUT_LEFT) {
            playCancelSound()
            builder.onLeftClickHandler?.invoke(state)
            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }

        val fullInputText = state.text
        var plainLabel = MiniMessage.miniMessage().stripTags(rawLabel)
        // Refined Regex to perfectly match valid Bukkit legacy codes only
        plainLabel = plainLabel.replace(Regex("(?i)§[0-9a-fk-or]"), "")

        val userInput = if (builder.keepLabel) {
            fullInputText
        } else if (fullInputText.startsWith(plainLabel, ignoreCase = true) && plainLabel.isNotEmpty()) {
            fullInputText.removePrefix(plainLabel).trim()
        } else {
            fullInputText.replace(plainLabel, "", true).trim()
        }

        if (userInput.isEmpty()) {
            playCancelSound()
            builder.onInvalidInputHandler?.invoke(player)
            return Collections.emptyList()
        }

        if (slot == AnvilGUI.Slot.OUTPUT) {
            val submitAlias = setting.submitSoundAlias ?: context.clickSoundAlias
            submitAlias?.let { alias ->
                GuiSound.playPack(player, SoundRegistry.get(alias))
            }

            builder.onOutputClickHandler?.invoke(state, userInput)
            return Collections.singletonList(AnvilGUI.ResponseAction.close())
        }

        return Collections.singletonList(AnvilGUI.ResponseAction.close())
    }

    private fun styleItem(item: GuiItem, context: GuiStyleSheet): GuiItem {
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