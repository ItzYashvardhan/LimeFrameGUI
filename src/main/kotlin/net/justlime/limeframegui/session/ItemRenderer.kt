package net.justlime.limeframegui.session

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.config.GuiDirectoryHandler.plugin
import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.engine.TextResolver
import net.justlime.limeframegui.enums.TextCase
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.util.SkullUtils
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

/**
 * Compiles a GuiItem blueprint into a renderable Bukkit ItemStack.
 * Executes the text resolution and styling pipeline against an isolated session context.
 */
object ItemRenderer {

    fun render(item: GuiItem, sessionContext: GuiStyleSheet, setting: IContextSetting): ItemStack {
        var activeItem = item
        val finalContext = mergeContexts(sessionContext, item.style)
        val viewer = finalContext.viewer ?: return item.baseItem.clone()

        // Handle Data-Driven Session States
        if (!item.stateId.isNullOrEmpty() && item.states.isNotEmpty()) {
            val resolvedState = TextResolver.resolve(viewer, item.stateId!!, setting, finalContext).uppercase()
            item.states[resolvedState]?.let { stateItem ->
                activeItem = stateItem
            }
        }
        val resultStack = activeItem.baseItem.clone()

        // Resolve Dynamic Material Strings
        if (activeItem.baseItemString.isNotBlank()) {
            val resolvedMaterialStr = TextResolver.resolve(viewer, activeItem.baseItemString, setting, finalContext).trim()
            val renderMaterial = Material.matchMaterial(resolvedMaterialStr.uppercase())

            if (renderMaterial != null) {
                resultStack.type = renderMaterial
            } else {
                resultStack.type = Material.STONE
                if (viewer.isOp) {
                    plugin.logger.warning("[LimeFrameGUI] Dynamic item resolved to invalid material '${resolvedMaterialStr}' for player ${viewer.name}.")
                }
            }
        }

        val meta = resultStack.itemMeta ?: return resultStack

        // Apply Typography and Meta Components
        val rawName = activeItem.currentName
        if (rawName.isNotEmpty()) {
            val resolvedName = TextResolver.resolve(viewer, rawName, setting, finalContext)
            meta.setDisplayName(FontStyle.applyStyle(resolvedName, finalContext, finalContext.textSettings.name))
        }

        val rawLore = activeItem.currentLore
        if (rawLore.isNotEmpty()) {
            val resolvedLore = TextResolver.resolveList(viewer, rawLore, setting, finalContext)
            meta.lore = FontStyle.applyStyle(resolvedLore, finalContext, finalContext.textSettings.lore)
        }

        if (meta is SkullMeta && !activeItem.texture.isNullOrEmpty()) {
            SkullUtils.applyDynamicTexture(meta, activeItem.texture!!, finalContext)
        }

        resultStack.itemMeta = meta
        return resultStack
    }

    /**
     * Isolates state by deriving a new style sheet.
     * Item-level properties strictly override session-level defaults.
     */
    private fun mergeContexts(base: GuiStyleSheet, override: GuiStyleSheet): GuiStyleSheet {
        val mergedTextSettings = base.textSettings.clone().apply {
            this.name = mergeTextRule(base.textSettings.name, override.textSettings.name)
            this.lore = mergeTextRule(base.textSettings.lore, override.textSettings.lore)
        }

        return base.copy(
            placeholder = (base.placeholder + override.placeholder).toMutableMap(),
            textSettings = mergedTextSettings,
            offlinePlayer = override.offlinePlayer ?: base.offlinePlayer,
            viewer = override.viewer ?: base.viewer
        )
    }

    /**
     * Helper to cleanly cascade typography overrides down to the base rules.
     */
    private fun mergeTextRule(base: TextFormatRule, override: TextFormatRule): TextFormatRule {
        return TextFormatRule(
            font = override.font ?: base.font ?: false,
            weights = override.weights ?: base.weights ?: emptyList(),
            textCase = override.textCase ?: base.textCase ?: TextCase.REGULAR,
            prefix = override.prefix ?: base.prefix ?: "",
            suffix = override.suffix ?: base.suffix ?: "",
            wrapLength = override.wrapLength ?: base.wrapLength ?: -1
        )
    }
}