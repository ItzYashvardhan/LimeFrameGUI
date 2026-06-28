package net.justlime.limeframegui.config.serializers

import net.justlime.limeframegui.config.FrameKeys
import net.justlime.limeframegui.util.FrameConverter
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object Base64Serializer {

    // --- READING ---

    fun loadItem(section: ConfigurationSection, key: String): ItemStack? {
        val encodedItem = section.getString(key) ?: return null
        return FrameConverter.deserializeItemStack(encodedItem)
    }

    fun loadItems(section: ConfigurationSection, key: String): List<ItemStack> {
        val encodedItems = section.getString(key) ?: return emptyList()
        return FrameConverter.deserializeItemStackList(encodedItems) ?: emptyList()
    }

    fun loadInventory(section: ConfigurationSection): Inventory? {
        val encodedInventory = section.getString(FrameKeys.Item.BASE64_DATA) ?: return null
        val setting = InventorySerializer.loadInventorySetting(section)
        val tempInventory = FrameConverter.deserializeInventory(encodedInventory) ?: return null

        val finalInventory = Bukkit.createInventory(null, setting.rows * 9, setting.title)
        finalInventory.contents = tempInventory.contents
        return finalInventory
    }

    // --- WRITING ---

    fun writeItem(section: ConfigurationSection, key: String, itemStack: ItemStack) {
        section.set(key, FrameConverter.serializeItemStack(itemStack))
    }

    fun writeItemsData(section: ConfigurationSection, item: ItemStack) {
        section.set(FrameKeys.Item.BASE64_DATA, FrameConverter.serializeItemStack(item))
    }

    fun writeInventory(
        section: ConfigurationSection,
        inventory: Inventory,
        title: String = FrameKeys.Default.DEFAULT_TITLE
    ) {
        ConfigWriter.writeInventorySettingsToSection(section, inventory.size / 9, title)
        section.set(FrameKeys.Item.BASE64_DATA, FrameConverter.serializeInventory(inventory))
    }
}