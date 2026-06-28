package net.justlime.limeframegui.config.serializers

import net.justlime.limeframegui.config.FrameKeys
import net.justlime.limeframegui.enums.TextCase
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.util.toGuiItem
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.Damageable

object ConfigWriter {

    fun writeItem(section: ConfigurationSection, item: GuiItem) {
        val base = item.baseItem
        val meta = base.itemMeta

        // 1. Raw Text Templates
        if (item.name.isNotEmpty()) section.set(FrameKeys.Item.NAME, item.name)
        if (item.lore.isNotEmpty()) section.set(FrameKeys.Item.LORE, item.lore)

        // 2. Visual ItemStack Properties
        section.set(FrameKeys.Item.MATERIAL, base.type.name)
        section.set(FrameKeys.Item.AMOUNT, base.amount)

        if (meta != null) {
            if (meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                section.set(FrameKeys.Item.GLOW, true)
            } else if (meta.hasEnchants()) {
                try {
                    if (meta.hasEnchantmentGlintOverride()) {
                        section.set(FrameKeys.Item.GLOW, meta.enchantmentGlintOverride)
                    }
                } catch (_: NoSuchMethodError) {
                }
            }

            if (meta.itemFlags.isNotEmpty()) {
                section.set(FrameKeys.Item.FLAGS, meta.itemFlags.map { it.name })
            }

            if (meta.hasCustomModelData()) {
                section.set(FrameKeys.Item.MODEL, meta.customModelData)
            }

            if (meta.isUnbreakable) {
                section.set(FrameKeys.Item.UNBREAKABLE, true)
            }

            if (meta is Damageable && meta.hasDamage()) {
                section.set(FrameKeys.Item.DAMAGE, meta.damage)
            }
        }

        // 3. LimeFrame Properties
        item.texture?.let { section.set(FrameKeys.Item.TEXTURE, it) }
        item.updateInterval?.let { section.set(FrameKeys.Item.UPDATE_INTERVAL, it) }

        item.slot?.let { section.set(FrameKeys.Item.SLOT, it) }
        if (item.slotList.isNotEmpty()) section.set(FrameKeys.Item.SLOTS, item.slotList)

        if (item.viewRequirements.isNotEmpty()) section.set(FrameKeys.Item.VIEW_REQUIREMENT, item.viewRequirements)
        if (item.priority != 0) section.set(FrameKeys.Item.PRIORITY, item.priority)

        // 4. Stylish Settings
        item.style.clickSoundAlias?.let { section.set(FrameKeys.Style.SOUND_CLICK, it) }

        val textSec = section.createSection(FrameKeys.Text.SECTION)
        writeRule(textSec, "name", item.style.textSettings.name)
        writeRule(textSec, "lore", item.style.textSettings.lore)
    }

    fun writeItems(section: ConfigurationSection, items: List<GuiItem>) {
        val itemsSection = section.createSection(FrameKeys.Item.SECTION)
        val itemMap = mutableMapOf<GuiItem, MutableList<Int>>()

        for ((index, item) in items.withIndex()) {
            itemMap.computeIfAbsent(item) { mutableListOf() }.add(item.slot ?: index)
        }

        for ((item, slots) in itemMap) {
            val key = slots.first().toString()
            val itemSection = itemsSection.createSection(key)
            val cleanItem = item.copy(slot = null, slotList = mutableListOf())

            writeItem(itemSection, cleanItem)

            if (slots.size > 1) {
                itemSection.set(FrameKeys.Item.SLOTS, slots)
            }
        }
    }


    fun writeInventorySetting(section: ConfigurationSection, setting: GuiSetting) {
        writeInventorySettingsToSection(section, setting.rows, setting.title, setting.style)
    }

    fun writeInventory(
        section: ConfigurationSection,
        inventory: Inventory,
        title: String = FrameKeys.Default.DEFAULT_TITLE
    ) {
        writeInventorySettingsToSection(section, inventory.size / 9, title)
        val itemsSection = section.createSection(FrameKeys.Item.SECTION)

        for (i in 0 until inventory.size) {
            val itemStack = inventory.getItem(i) ?: continue
            val guiItem = itemStack.toGuiItem()
            writeItem(itemsSection.createSection(i.toString()), guiItem)
        }
    }

    fun writeInventorySettingsToSection(
        section: ConfigurationSection,
        rows: Int,
        title: String,
        style: GuiStyleSheet? = null
    ) {
        section.set(FrameKeys.Main.TITLE, title)
        section.set(FrameKeys.Main.ROWS, rows)

        if (style != null) {
            style.clickSoundAlias?.let { section.set(FrameKeys.Style.SOUND_CLICK, it) }
            style.openSoundAlias?.let { section.set(FrameKeys.Style.SOUND_OPEN, it) }
            style.closeSoundAlias?.let { section.set(FrameKeys.Style.SOUND_CLOSE, it) }
        }
    }

    private fun writeRule(textSec: ConfigurationSection, path: String, rule: TextFormatRule) {
        val sub = textSec.createSection(path)
        sub.set(FrameKeys.Text.FONT, rule.font)
        if (!rule.prefix.isNullOrEmpty()) sub.set(FrameKeys.Text.PREFIX, rule.prefix)
        if (!rule.suffix.isNullOrEmpty()) sub.set(FrameKeys.Text.SUFFIX, rule.suffix)
        if (rule.wrapLength != -1) sub.set(FrameKeys.Text.WRAP_LENGTH, rule.wrapLength)
        if (!rule.weights.isNullOrEmpty()) sub.set(FrameKeys.Text.WEIGHT, rule.weights)
        if (rule.textCase != TextCase.REGULAR) {
            sub.set(FrameKeys.Text.CASE, rule.textCase?.name?.lowercase()?.replace("_", "-"))
        }
    }
}