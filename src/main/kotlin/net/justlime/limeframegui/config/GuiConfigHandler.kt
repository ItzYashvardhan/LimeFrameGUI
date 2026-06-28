package net.justlime.limeframegui.config

import net.justlime.limeframegui.config.serializers.Base64Serializer
import net.justlime.limeframegui.config.serializers.ConfigWriter
import net.justlime.limeframegui.config.serializers.InventorySerializer
import net.justlime.limeframegui.config.serializers.ItemSerializer
import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

// @formatter:off
/**
 * The Facade Engine for data conversion.
 * Routes parsing and serialization requests to highly-specialized, single-responsibility handlers.
 */
object GuiConfigHandler {

    // ITEM READING
    fun loadItem(section: ConfigurationSection): GuiItem = ItemSerializer.loadItem(section)
    fun loadItems(section: ConfigurationSection): List<GuiItem> = ItemSerializer.loadItems(section)

    // INVENTORY READING
    fun loadInventory(section: ConfigurationSection): Inventory = InventorySerializer.loadInventory(section)
    fun loadInventorySetting(section: ConfigurationSection?): GuiSetting = InventorySerializer.loadInventorySetting(section)
    fun loadAnvilSetting(section: ConfigurationSection?): AnvilGuiSetting = InventorySerializer.loadAnvilSetting(section)

    // CONFIG WRITING
    fun writeItemToSection(section: ConfigurationSection, item: GuiItem) = ConfigWriter.writeItem(section, item)
    fun writeItemsToSection(section: ConfigurationSection, items: List<GuiItem>) = ConfigWriter.writeItems(section, items)
    fun writeInventorySettingToSection(section: ConfigurationSection, setting: GuiSetting) = ConfigWriter.writeInventorySetting(section, setting)
    fun writeInventoryToSection(section: ConfigurationSection, inventory: Inventory, title: String = FrameKeys.Default.DEFAULT_TITLE) = ConfigWriter.writeInventory(section, inventory, title)

    // BASE64 READING
    fun loadItemBase64(section: ConfigurationSection, key: String): ItemStack? = Base64Serializer.loadItem(section, key)
    fun loadItemsBase64(section: ConfigurationSection, key: String): List<ItemStack> = Base64Serializer.loadItems(section, key)
    fun loadInventoryBase64(section: ConfigurationSection): Inventory? = Base64Serializer.loadInventory(section)

    // BASE64 WRITING
    fun writeItemBase64(section: ConfigurationSection, key: String, itemStack: ItemStack) = Base64Serializer.writeItem(section, key, itemStack)
    fun writeItemsBase64(section: ConfigurationSection, item: ItemStack) = Base64Serializer.writeItemsData(section, item)
    fun writeInventoryBase64ToSection(section: ConfigurationSection, inventory: Inventory, title: String = FrameKeys.Default.DEFAULT_TITLE) = Base64Serializer.writeInventory(section, inventory, title)
}