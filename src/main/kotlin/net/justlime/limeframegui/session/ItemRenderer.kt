package net.justlime.limeframegui.session

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.engine.TextResolver
import net.justlime.limeframegui.enums.TextCase
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.util.SkullUtils
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

/**
 * Compiles a GuiItem blueprint into a renderable Bukkit ItemStack.
 * Executes the text resolution and styling pipeline against an isolated session context.
 */
object ItemRenderer {

    fun render(item: GuiItem, sessionContext: GuiStyleSheet, setting: IContextSetting): ItemStack {
        val resultStack = item.baseItem.clone()
        val meta = resultStack.itemMeta ?: return resultStack

        val finalContext = mergeContexts(sessionContext, item.style)
        val viewer = finalContext.viewer ?: return resultStack

        val rawName = item.currentName
        if (rawName.isNotEmpty()) {
            val resolvedName = TextResolver.resolve(viewer, rawName, setting, finalContext)
            meta.setDisplayName(FontStyle.applyStyle(resolvedName, finalContext, finalContext.textSettings.name))
        }

        val rawLore = item.currentLore
        if (rawLore.isNotEmpty()) {
            val resolvedLore = TextResolver.resolveList(viewer, rawLore, setting, finalContext)
            meta.lore = FontStyle.applyStyle(resolvedLore, finalContext, finalContext.textSettings.lore)
        }

        if (meta is SkullMeta && !item.texture.isNullOrEmpty()) {
            SkullUtils.applyDynamicTexture(meta, item.texture!!, finalContext)
        }

        resultStack.itemMeta = meta
        return resultStack
    }

    /**
     * Isolates state by deriving a new style sheet.
     * Item-level properties strictly override session-level defaults.
     */
    private fun mergeContexts(base: GuiStyleSheet, override: GuiStyleSheet): GuiStyleSheet {

        val mergedNameRule = TextFormatRule(
            font = override.textSettings.name.font ?: base.textSettings.name.font ?: false,
            weights = override.textSettings.name.weights ?: base.textSettings.name.weights ?: emptyList(),
            textCase = override.textSettings.name.textCase ?: base.textSettings.name.textCase ?: TextCase.REGULAR,
            prefix = override.textSettings.name.prefix ?: base.textSettings.name.prefix ?: "",
            suffix = override.textSettings.name.suffix ?: base.textSettings.name.suffix ?: "",
            wrapLength = override.textSettings.name.wrapLength ?: base.textSettings.name.wrapLength ?: -1
        )

        val mergedLoreRule = TextFormatRule(
            font = override.textSettings.lore.font ?: base.textSettings.lore.font ?: false,
            weights = override.textSettings.lore.weights ?: base.textSettings.lore.weights ?: emptyList(),
            textCase = override.textSettings.lore.textCase ?: base.textSettings.lore.textCase ?: TextCase.REGULAR,
            prefix = override.textSettings.lore.prefix ?: base.textSettings.lore.prefix ?: "",
            suffix = override.textSettings.lore.suffix ?: base.textSettings.lore.suffix ?: "",
            wrapLength = override.textSettings.lore.wrapLength ?: base.textSettings.lore.wrapLength ?: -1
        )

        val mergedTextSettings = base.textSettings.clone().apply {
            this.name = mergedNameRule
            this.lore = mergedLoreRule
        }

        return base.copy(
            placeholder = (base.placeholder + override.placeholder).toMutableMap(),
            textSettings = mergedTextSettings,
            offlinePlayer = override.offlinePlayer ?: base.offlinePlayer,
            viewer = override.viewer ?: base.viewer
        )
    }
}