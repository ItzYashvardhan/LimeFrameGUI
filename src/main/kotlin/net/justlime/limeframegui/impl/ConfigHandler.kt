package net.justlime.limeframegui.impl

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.models.GUISetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSound
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.utilities.FrameConverter
import net.justlime.limeframegui.utilities.toGuiItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.io.File

@Suppress("unused")
class ConfigHandler(private val filename: String, private val dataFolder: File = LimeFrameAPI.getPlugin().dataFolder) {

    private val keys = LimeFrameAPI.keys

    private var file = File(dataFolder, filename)
    var config: YamlConfiguration = loadYaml()

    fun reload(): Boolean {
        val reloadedFile = File(dataFolder, filename)
        return if (reloadedFile.exists()) {
            config = YamlConfiguration.loadConfiguration(reloadedFile)
            file = reloadedFile
            true
        } else false
    }

    private fun save(): Boolean {
        return runCatching { config.save(file) }.isSuccess
    }

    private fun loadYaml(): YamlConfiguration {
        return YamlConfiguration.loadConfiguration(file)
    }

    fun saveItem(path: String, item: GuiItem, section: ConfigurationSection) {
        writeItemToSection(section, item)
    }

    fun saveItemBase64(path: String, itemStack: ItemStack): Boolean {
        val encodedItem = FrameConverter.serializeItemStack(itemStack)
        config.set(path, encodedItem)
        return save()
    }

    fun loadItem(path: String): GuiItem? {
        val section = config.getConfigurationSection(path) ?: return null

        return with(section) {

            val sound = getString(keys.stylishItemSound) ?: getStringList(keys.stylishItemSound)

            GuiItem(
                material = Material.getMaterial(getString(keys.material) ?: "AIR") ?: Material.AIR,
                name = getString(keys.name) ?: "",
                lore = getStringList(keys.lore),
                glow = getBoolean(keys.glow, false),
                flags = getStringList(keys.flags).mapNotNull { runCatching { ItemFlag.valueOf(it) }.getOrNull() },
                customModelData = takeIf { contains(keys.model) }?.getInt(keys.model),
                amount = getInt(keys.amount, 1),
                texture = getString(keys.texture),
                slot = getString(keys.slot)?.toIntOrNull(),
                slotList = getIntegerList(keys.slotList),
                unbreakable = getBoolean(keys.unbreakable, false),
                damage = takeIf { contains(keys.damage) }?.getInt(keys.damage),
                style = GuiStyleSheet(
                    stylishName = takeIf { contains(keys.stylishFontName) }?.getBoolean(keys.stylishFontName) ?: keys.stylishName,
                    stylishLore = takeIf { contains(keys.stylishFontLore) }?.getBoolean(keys.stylishFontLore) ?: keys.stylishLore,
                    clickSound = takeIf { contains(keys.stylishItemSound) }?.let { GuiSound.loadSound(sound) } ?: keys.clickSound),
            )
        }
    }

    fun loadItemBase64(path: String): ItemStack? {
        val encodedItem = config.getString(path)
        return FrameConverter.deserializeItemStack(encodedItem)
    }

    fun saveItems(path: String, items: List<GuiItem>): Boolean {
        val baseSection = config.createSection(path)

        val itemsSection = baseSection.createSection(keys.inventoryItemSection)
        val itemMap = mutableMapOf<GuiItem, MutableList<Int>>()

        for ((index, item) in items.withIndex()) {
            itemMap.computeIfAbsent(item) { mutableListOf() }.add(item.slot ?: index)
        }

        cleanSaveItemMap(itemMap, itemsSection)


        return save()
    }

    fun saveItemsBase64(path: String, item: ItemStack): Boolean {

        val section = config.createSection(path)
        val encodedItem = FrameConverter.serializeItemStack(item)
        section.set(keys.base64Data, encodedItem)

        return save()
    }

    fun loadItems(path: String): List<GuiItem> {
        val section = config.getConfigurationSection(path) ?: return emptyList<GuiItem>()

        return section.getKeys(false).mapNotNull { key ->
            section.getConfigurationSection(key)?.let {
                loadItem("$path.$key")
            }
        }
    }

    fun loadItemsBase64(path: String): List<ItemStack> {
        val encodedItems = config.getString(path) ?: return emptyList<ItemStack>()
        return FrameConverter.deserializeItemStackList(encodedItems) ?: emptyList()
    }

    fun loadInventory(path: String): Inventory? {
        val section = config.getConfigurationSection(path) ?: return null
        val setting = loadInventorySetting(path)

        val inventory = Bukkit.createInventory(null, setting.rows * 9, setting.title)
        val itemsSection = section.getConfigurationSection(keys.inventoryItemSection) ?: return inventory

        for (key in itemsSection.getKeys(false)) {
            val slot = key.toIntOrNull() ?: continue
            val itemPath = "$path.${keys.inventoryItemSection}.$key"
            val item = loadItem(itemPath) ?: continue
            inventory.setItem(slot, item.toItemStack())
        }

        return inventory
    }

    fun loadInventorySetting(path: String): GUISetting {
        val section = config.getConfigurationSection(path) ?: return GUISetting(keys.defaultInventoryRows, keys.defaultInventoryTitle)
        val title = section.getString(keys.inventoryTitle, keys.defaultInventoryTitle) ?: keys.defaultInventoryTitle
        val rows = section.getInt(keys.inventoryRows, keys.defaultInventoryRows)
        val stylishTitle = section.getBoolean(keys.stylishFontTitle, keys.stylishTitle)
        val stylishName = section.getBoolean(keys.stylishFontName, keys.stylishName)
        val stylishLore = section.getBoolean(keys.stylishFontLore, keys.stylishLore)

        val clickSoundString = section.getString(keys.stylishItemSound) ?: section.getStringList(keys.stylishItemSound)
        val clickSound = GuiSound.loadSound(clickSoundString)
        val openSoundString = section.getString(keys.stylishOpenSound) ?: section.getStringList(keys.stylishOpenSound)
        val openSound = GuiSound.loadSound(openSoundString)
        val closeSoundString = section.getString(keys.stylishCloseSound) ?: section.getStringList(keys.stylishCloseSound)
        val closeSound = GuiSound.loadSound(closeSoundString)


        return GUISetting(rows, title, GuiStyleSheet(stylishTitle = stylishTitle, stylishName = stylishName, stylishLore = stylishLore, clickSound = clickSound, openSound = openSound, closeSound = closeSound))

    }

    fun saveInventory(path: String, inventory: Inventory, inventoryTitle: String = keys.defaultInventoryTitle): Boolean {
        val section = getSection(path)
        writeInventorySettingsToSection(section, inventory.size / 9, inventoryTitle)
        val itemsSection = section.createSection(keys.inventoryItemSection)
        for (i in 0 until inventory.size) {
            val itemStack = inventory.getItem(i) ?: continue
            val guiItem = itemStack.toGuiItem()
            saveItem("$path.${keys.inventoryItemSection}.$i", guiItem, itemsSection.createSection(i.toString()))
        }
        return save()
    }

    fun saveInventorySetting(path: String, setting: GUISetting): Boolean {
        val section = getSection(path)
        writeInventorySettingsToSection(section, setting.rows, setting.title)
        return save()
    }

    fun saveInventoryBase64(path: String, inventory: Inventory, title: String = keys.defaultInventoryTitle): Boolean {
        val section = getSection(path)
        writeInventorySettingsToSection(section, inventory.size / 9, title)
        val encodedInventory = FrameConverter.serializeInventory(inventory)
        section.set(keys.base64Data, encodedInventory)
        return save()
    }

    fun loadInventoryBase64(path: String): Inventory? {
        val section = config.getConfigurationSection(path) ?: return null
        val encodedInventory = section.getString(keys.base64Data) ?: return null
        val setting = loadInventorySetting(path)
        val tempInventory = FrameConverter.deserializeInventory(encodedInventory) ?: return null
        val finalInventory = Bukkit.createInventory(null, setting.rows * 9, setting.title)
        finalInventory.contents = tempInventory.contents
        return finalInventory
    }

    private fun cleanSaveItemMap(itemMap: MutableMap<GuiItem, MutableList<Int>>, itemsSection: ConfigurationSection) {
        for ((item, slots) in itemMap) {
            val key = slots.first().toString()
            val itemSection = itemsSection.createSection(key)

            // Clear slot info before saving to avoid redundant fields
            val cleanItem = item.copy(slot = null, slotList = mutableListOf())
            writeItemToSection(itemSection, cleanItem)

            if (slots.size > 1) {
                itemSection.set(keys.slotList, slots)
            }
        }
    }

    private fun getSection(path: String): ConfigurationSection {
        return config.getConfigurationSection(path) ?: config.createSection(path)
    }

    private fun writeItemToSection(section: ConfigurationSection, item: GuiItem) {
        section.set(keys.name, item.name)
        section.set(keys.material, item.material.name)
        section.set(keys.lore, item.lore)
        section.set(keys.glow, item.glow)
        section.set(keys.flags, item.flags.map { it.name })
        section.set(keys.model, item.customModelData)
        section.set(keys.texture, item.texture)
        section.set(keys.amount, item.amount)
        section.set(keys.unbreakable, item.unbreakable)
        section.set(keys.damage, item.damage)
        item.style.stylishName.let { section.set(keys.stylishFontName, it) }
        item.style.stylishLore.let { section.set(keys.stylishFontLore, it) }
        item.slot?.let { section.set(keys.slot, it) }
        if (item.slotList.isNotEmpty()) section.set(keys.slotList, item.slotList)
        item.style.clickSound.let { if (!it.isEmpty()) section.set(keys.stylishItemSound, "${it.sound},${it.pitch},${it.volume}") }
    }

    private fun writeInventorySettingsToSection(section: ConfigurationSection, rows: Int, title: String) {
        section.set(keys.inventoryTitle, title)
        section.set(keys.inventoryRows, rows)
        section.set(keys.stylishFontTitle, LimeFrameAPI.keys.stylishTitle)
        section.set(keys.stylishFontName, LimeFrameAPI.keys.stylishName)
        section.set(keys.stylishFontLore, LimeFrameAPI.keys.stylishLore)
        if (!LimeFrameAPI.keys.clickSound.isEmpty()) section.set(keys.stylishItemSound, "${LimeFrameAPI.keys.clickSound.sound},${LimeFrameAPI.keys.clickSound.pitch},${LimeFrameAPI.keys.clickSound.volume}")
        if (!LimeFrameAPI.keys.openSound.isEmpty()) section.set(keys.stylishOpenSound, "${LimeFrameAPI.keys.openSound.sound},${LimeFrameAPI.keys.openSound.pitch},${LimeFrameAPI.keys.openSound.volume}")
        if (!LimeFrameAPI.keys.closeSound.isEmpty()) section.set(keys.stylishCloseSound, "${LimeFrameAPI.keys.closeSound.sound},${LimeFrameAPI.keys.closeSound.pitch},${LimeFrameAPI.keys.closeSound.volume}")

    }
}