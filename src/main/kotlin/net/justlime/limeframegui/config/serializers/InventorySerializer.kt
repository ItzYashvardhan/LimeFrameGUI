package net.justlime.limeframegui.config.serializers

import net.justlime.limeframegui.config.FrameKeys
import net.justlime.limeframegui.models.*
import net.justlime.limeframegui.registry.common.TemplateCompiler
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.Inventory

object InventorySerializer {

    fun loadInventorySetting(section: ConfigurationSection?): GuiSetting {
        if (section == null) return GuiSetting(FrameKeys.Default.DEFAULT_CHEST_ROWS, FrameKeys.Default.DEFAULT_CHEST_TITLE)

        return GuiSetting(
            rows = section.getInt(FrameKeys.Main.ROWS, FrameKeys.Default.DEFAULT_CHEST_ROWS),
            title = section.getString(FrameKeys.Main.TITLE, FrameKeys.Default.DEFAULT_CHEST_TITLE)
                ?: FrameKeys.Default.DEFAULT_CHEST_TITLE,
            style = GuiStyleSheet(
                textSettings = guiTextSettings(section),
                clickSoundAlias = section.getString(FrameKeys.Sound.SOUND_CLICK)
                    ?: section.getStringList(FrameKeys.Sound.SOUND_CLICK).firstOrNull(),
                openSoundAlias = section.getString(FrameKeys.Sound.SOUND_OPEN)
                    ?: section.getStringList(FrameKeys.Sound.SOUND_OPEN).firstOrNull(),
                closeSoundAlias = section.getString(FrameKeys.Sound.SOUND_CLOSE)
                    ?: section.getStringList(FrameKeys.Sound.SOUND_CLOSE).firstOrNull()
            )
        )
    }

    fun loadInventory(section: ConfigurationSection): Inventory {
        val setting = loadInventorySetting(section)
        val inventory = Bukkit.createInventory(null, setting.rows * 9, setting.title)
        val itemsSection = section.getConfigurationSection(FrameKeys.Item.SECTION) ?: return inventory

        for (key in itemsSection.getKeys(false)) {
            val slot = key.toIntOrNull() ?: continue
            itemsSection.getConfigurationSection(key)?.let { itemSection ->
                val item = ItemSerializer.loadItem(itemSection)
                inventory.setItem(slot, item.baseItem.clone())
            }
        }
        return inventory
    }

    //@formatter:off
    fun loadAnvilSetting(section: ConfigurationSection?): AnvilGuiSetting {

        val setting = AnvilGuiSetting()

        if (section == null) return setting

        for(key in section.getKeys(false)){
            if (key == FrameKeys.Main.SECTION) {
                val mainSec = section.getConfigurationSection(FrameKeys.Main.SECTION)
                setting.title = mainSec?.getString(FrameKeys.Main.TITLE, FrameKeys.Default.DEFAULT_ANVIL_TITLE) ?: FrameKeys.Default.DEFAULT_ANVIL_TITLE
                setting.label =  mainSec?.getString(FrameKeys.Main.LABEL, FrameKeys.Default.DEFAULT_ANVIL_LABEL) ?: FrameKeys.Default.DEFAULT_ANVIL_LABEL
                setting.openSoundString = mainSec?.getString(FrameKeys.Sound.SOUND_OPEN) ?: section.getStringList(FrameKeys.Sound.SOUND_OPEN).firstOrNull()
                setting.preventClose = mainSec?.getBoolean(FrameKeys.Main.PREVENT_CLOSE, false) ?: false
                continue
            }
            val itemSection = section.getConfigurationSection(key) ?: continue
            val item = ItemSerializer.loadItem(itemSection)
            val itemType = itemSection.getString(FrameKeys.Anvil.TYPE)

            when(itemType?.lowercase()){
                FrameKeys.Anvil.TYPE_LEFT -> setting.leftItem = item
                FrameKeys.Anvil.TYPE_RIGHT -> setting.rightItem = item
                FrameKeys.Anvil.TYPE_OUTPUT -> setting.outPutItem = item
            }
        }
        return setting
    }
    //@formatter:on

    private fun guiTextSettings(section: ConfigurationSection): GuiTextSettings = GuiTextSettings(
        title = section.getConfigurationSection(FrameKeys.Text.SECTION)?.let { textSec ->
            val rule = TextFormatRule(font = false)
            TemplateCompiler.parseTextGroupRule(textSec, "title", rule) // <-- Removed LimeFrameAPI.keys
            rule
        } ?: TextFormatRule(font = false),

        name = section.getConfigurationSection(FrameKeys.Text.SECTION)?.let { textSec ->
            val rule = TextFormatRule(font = false)
            TemplateCompiler.parseTextGroupRule(textSec, "name", rule) // <-- Removed LimeFrameAPI.keys
            rule
        } ?: TextFormatRule(font = false),

        lore = section.getConfigurationSection(FrameKeys.Text.SECTION)?.let { textSec ->
            val rule = TextFormatRule(font = false)
            TemplateCompiler.parseTextGroupRule(textSec, "lore", rule) // <-- Removed LimeFrameAPI.keys
            rule
        } ?: TextFormatRule(font = false)
    )

}