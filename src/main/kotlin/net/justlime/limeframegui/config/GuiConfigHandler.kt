package net.justlime.limeframegui.config

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.models.*
import net.justlime.limeframegui.registry.gui.TemplateCompiler
import net.justlime.limeframegui.util.FrameConverter
import net.justlime.limeframegui.util.toGuiItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/**
 * The serialization and parsing engine for structural data types.
 * Converts raw YAML configurations into rich memory objects (GuiItem, GuiSetting, Inventory)
 * and handles base64 binary stream translation.
 */
@Suppress("unused")
object GuiConfigHandler {

    private val keys get() = LimeFrameAPI.keys

    /**
     * Parses a ConfigurationSection into a context-ready [GuiItem], dynamically matching
     * custom configured mapping keys.
     */
    fun loadItem(section: ConfigurationSection): GuiItem {
        val soundAlias =
            section.getString(keys.stylishItemSound) ?: section.getStringList(keys.stylishItemSound).firstOrNull()
        val parsedAction = section.getString(keys.action)
        return GuiItem(
            material = Material.getMaterial(section.getString(keys.material) ?: "AIR") ?: Material.AIR,
            name = section.getString(keys.name) ?: "",
            lore = section.getStringList(keys.lore),
            updateInterval = section.getString(keys.updateInterval)?.toIntOrNull(),
            glow = section.getBoolean(keys.glow, false),
            flags = section.getStringList(keys.flags).mapNotNull { runCatching { ItemFlag.valueOf(it) }.getOrNull() },
            customModelData = section.takeIf { it.contains(keys.model) }?.getInt(keys.model),
            amount = section.getInt(keys.amount, 1),
            texture = section.getString(keys.texture),
            slot = section.getString(keys.slot)?.toIntOrNull(),
            slotList = section.getIntegerList(keys.slotList),
            unbreakable = section.getBoolean(keys.unbreakable, false),
            damage = section.takeIf { it.contains(keys.damage) }?.getInt(keys.damage),
            style = GuiStyleSheet(
                textSettings = GuiTextSettings(
                    name = section.getConfigurationSection(keys.textSection)?.let { textSec ->
                        val rule = TextFormatRule(font = keys.stylishName)
                        TemplateCompiler.parseTextGroupRule(textSec, "name", rule, keys)
                        rule
                    } ?: GuiTextSettings().name,
                    lore = section.getConfigurationSection(keys.textSection)?.let { textSec ->
                        val rule = TextFormatRule(font = keys.stylishLore)
                        TemplateCompiler.parseTextGroupRule(textSec, "lore", rule, keys)
                        rule
                    } ?: GuiTextSettings().lore
                ),
                clickSoundAlias = soundAlias,
                action = parsedAction
            ),
            viewRequirements = section.getStringList("view-requirement"),
            priority = section.getInt("priority", 0),

        )
    }

    /**
     * Loops through a parent configuration block to parse multiple items simultaneously.
     */
    fun loadItems(section: ConfigurationSection): List<GuiItem> {
        return section.getKeys(false).mapNotNull { key ->
            section.getConfigurationSection(key)?.let { loadItem(it) }
        }
    }

    /**
     * Loads base structural rules for inventory creation (size, headers, and universal styling filters).
     */
    fun loadInventorySetting(section: ConfigurationSection?): GuiSetting {
        if (section == null) return GuiSetting(keys.defaultInventoryRows, keys.defaultInventoryTitle)

        return GuiSetting(
            rows = section.getInt(keys.inventoryRows, keys.defaultInventoryRows),
            title = section.getString(keys.inventoryTitle, keys.defaultInventoryTitle) ?: keys.defaultInventoryTitle,
            style = GuiStyleSheet(
                textSettings = guiTextSettings(section),

                clickSoundAlias = section.getString(keys.stylishItemSound)
                    ?: section.getStringList(keys.stylishItemSound).firstOrNull(),
                openSoundAlias = section.getString(keys.stylishOpenSound)
                    ?: section.getStringList(keys.stylishOpenSound).firstOrNull(),
                closeSoundAlias = section.getString(keys.stylishCloseSound)
                    ?: section.getStringList(keys.stylishCloseSound).firstOrNull()
            )
        )
    }


    /**
     * Assembles a standard Bukkit inventory populated directly with flat, indexed configuration parameters.
     */
    fun loadInventory(section: ConfigurationSection): Inventory {
        val setting = loadInventorySetting(section)
        val inventory = Bukkit.createInventory(null, setting.rows * 9, setting.title)
        val itemsSection = section.getConfigurationSection(keys.inventoryItemSection) ?: return inventory

        for (key in itemsSection.getKeys(false)) {
            val slot = key.toIntOrNull() ?: continue
            itemsSection.getConfigurationSection(key)?.let { itemSection ->
                val item = loadItem(itemSection)
                inventory.setItem(slot, item.toItemStack())
            }
        }
        return inventory
    }

    /**
     * Formats settings required to build context-aware, text-input Anvil UI panels.
     */
    fun loadAnvilSetting(section: ConfigurationSection?): AnvilGuiSetting {
        if (section == null) return AnvilGuiSetting(
            title = keys.defaultAnvilTitle,
            label = keys.defaultAnvilLabel,
            leftItem = GuiItem(Material.AIR),
            rightItem = GuiItem(Material.AIR),
            outPutItem = GuiItem(Material.AIR),
            style = GuiStyleSheet()
        )

        val leftItem =
            section.getConfigurationSection(keys.anvilLeftItem)?.let { loadItem(it) } ?: GuiItem(Material.AIR)
        val rightItem =
            section.getConfigurationSection(keys.anvilRightItem)?.let { loadItem(it) } ?: GuiItem(Material.AIR)
        val outputItem =
            section.getConfigurationSection(keys.anvilOutputItem)?.let { loadItem(it) } ?: GuiItem(Material.AIR)

        return AnvilGuiSetting(
            title = section.getString(keys.anvilTitle, keys.defaultAnvilTitle) ?: keys.defaultAnvilTitle,
            label = section.getString(keys.anvilLabel, keys.defaultAnvilLabel) ?: keys.defaultAnvilLabel,
            preventClose = section.getBoolean(keys.anvilPreventClose, false),
            leftItem = leftItem,
            rightItem = rightItem,
            outPutItem = outputItem,
            openSoundAlias = section.getString(keys.stylishOpenSound) ?: section.getStringList(keys.stylishOpenSound)
                .firstOrNull(),
            cancelSoundAlias = section.getString(keys.anvilCancelSound) ?: section.getStringList(keys.anvilCancelSound)
                .firstOrNull(),
            submitSoundAlias = section.getString(keys.anvilSubmitSound) ?: section.getStringList(keys.anvilSubmitSound)
                .firstOrNull(),
            style = GuiStyleSheet(
                textSettings = guiTextSettings(section)
            )
        )
    }


    /**
     * Converts an encoded Base64 serialization block into a usable, individual [ItemStack].
     */
    fun loadItemBase64(section: ConfigurationSection, key: String): ItemStack? {
        val encodedItem = section.getString(key) ?: return null
        return FrameConverter.deserializeItemStack(encodedItem)
    }

    /**
     * Translates a collection string sequence block back into a raw array list of [ItemStack].
     */
    fun loadItemsBase64(section: ConfigurationSection, key: String): List<ItemStack> {
        val encodedItems = section.getString(key) ?: return emptyList()
        return FrameConverter.deserializeItemStackList(encodedItems) ?: emptyList()
    }

    /**
     * Restores an active inventory content array completely out of a Base64 system string layout.
     */
    fun loadInventoryBase64(section: ConfigurationSection): Inventory? {
        val encodedInventory = section.getString(keys.base64Data) ?: return null
        val setting = loadInventorySetting(section)
        val tempInventory = FrameConverter.deserializeInventory(encodedInventory) ?: return null

        val finalInventory = Bukkit.createInventory(null, setting.rows * 9, setting.title)
        finalInventory.contents = tempInventory.contents
        return finalInventory
    }

    /**
     * Serializes a runtime [GuiItem] object into structured text keys inside a file configuration sector.
     */
    fun writeItemToSection(section: ConfigurationSection, item: GuiItem) {
        section.set(keys.name, item.name)
        section.set(keys.material, item.material.name)
        section.set(keys.lore, item.lore)
        section.set(keys.updateInterval, item.updateInterval)
        section.set(keys.glow, item.glow)
        section.set(keys.flags, item.flags.map { it.name })
        section.set(keys.model, item.customModelData)
        section.set(keys.texture, item.texture)
        section.set(keys.amount, item.amount)
        section.set(keys.unbreakable, item.unbreakable)
        section.set(keys.damage, item.damage)
        val textSec = section.createSection(keys.textSection)

        writeRule(textSec,"name", item.style.textSettings.name)
        writeRule(textSec, "lore", item.style.textSettings.lore)
        item.slot?.let { section.set(keys.slot, it) }
        if (item.slotList.isNotEmpty()) section.set(keys.slotList, item.slotList)
        item.style.clickSoundAlias?.let { section.set(keys.stylishItemSound, it) }

        if (item.viewRequirements.isNotEmpty()) section.set("view-requirement", item.viewRequirements)
        if (item.priority != 0) section.set("priority", item.priority)
    }


    /**
     * Compresses and groups multiple [GuiItem] blueprints cleanly into localized item index lists.
     */
    fun writeItemsToSection(section: ConfigurationSection, items: List<GuiItem>) {
        val itemsSection = section.createSection(keys.inventoryItemSection)
        val itemMap = mutableMapOf<GuiItem, MutableList<Int>>()

        for ((index, item) in items.withIndex()) {
            itemMap.computeIfAbsent(item) { mutableListOf() }.add(item.slot ?: index)
        }
        cleanSaveItemMap(itemMap, itemsSection)
    }

    /**
     * Exports raw inventory size and visual settings metadata safely to a destination config zone.
     */
    fun writeInventorySettingToSection(section: ConfigurationSection, setting: GuiSetting) {
        writeInventorySettingsToSection(section, setting.rows, setting.title)
    }

    /**
     * Transforms an active living inventory canvas block completely into structured configuration lines.
     */
    fun writeInventoryToSection(
        section: ConfigurationSection,
        inventory: Inventory,
        title: String = keys.defaultInventoryTitle
    ) {
        writeInventorySettingsToSection(section, inventory.size / 9, title)
        val itemsSection = section.createSection(keys.inventoryItemSection)

        for (i in 0 until inventory.size) {
            val itemStack = inventory.getItem(i) ?: continue
            val guiItem = itemStack.toGuiItem()
            writeItemToSection(itemsSection.createSection(i.toString()), guiItem)
        }
    }

    /**
     * Serializes an individual [ItemStack] out to a specified Base64 configuration location string key.
     */
    fun writeItemBase64(section: ConfigurationSection, key: String, itemStack: ItemStack) {
        section.set(key, FrameConverter.serializeItemStack(itemStack))
    }

    /**
     * Pushes item binary byte sequences directly to default dynamic data destinations.
     */
    fun writeItemsBase64(section: ConfigurationSection, item: ItemStack) {
        section.set(keys.base64Data, FrameConverter.serializeItemStack(item))
    }

    /**
     * Packs total inventory dimensions and underlying item layout contents directly into compressed Base64 notation.
     */
    fun writeInventoryBase64ToSection(
        section: ConfigurationSection,
        inventory: Inventory,
        title: String = keys.defaultInventoryTitle
    ) {
        writeInventorySettingsToSection(section, inventory.size / 9, title)
        section.set(keys.base64Data, FrameConverter.serializeInventory(inventory))
    }

    /**
     * Simplifies configuration clutter by consolidating duplicate items occupying distinct positions.
     */
    private fun cleanSaveItemMap(itemMap: MutableMap<GuiItem, MutableList<Int>>, itemsSection: ConfigurationSection) {
        for ((item, slots) in itemMap) {
            val key = slots.first().toString()
            val itemSection = itemsSection.createSection(key)
            val cleanItem = item.copy(slot = null, slotList = mutableListOf())

            writeItemToSection(itemSection, cleanItem)

            if (slots.size > 1) {
                itemSection.set(keys.slotList, slots)
            }
        }
    }

    /**
     * Populates inventory structural headers alongside operational audio feedback assignments.
     */
    private fun writeInventorySettingsToSection(section: ConfigurationSection, rows: Int, title: String) {
        section.set(keys.inventoryTitle, title)
        section.set(keys.inventoryRows, rows)
        if (!LimeFrameAPI.keys.clickSound.isEmpty()) section.set(
            keys.stylishItemSound,
            "${LimeFrameAPI.keys.clickSound.sound},${LimeFrameAPI.keys.clickSound.pitch},${LimeFrameAPI.keys.clickSound.volume}"
        )
        if (!LimeFrameAPI.keys.openSound.isEmpty()) section.set(
            keys.stylishOpenSound,
            "${LimeFrameAPI.keys.openSound.sound},${LimeFrameAPI.keys.openSound.pitch},${LimeFrameAPI.keys.openSound.volume}"
        )
        if (!LimeFrameAPI.keys.closeSound.isEmpty()) section.set(
            keys.stylishCloseSound,
            "${LimeFrameAPI.keys.closeSound.sound},${LimeFrameAPI.keys.closeSound.pitch},${LimeFrameAPI.keys.closeSound.volume}"
        )
    }

    private fun writeRule(textSec: ConfigurationSection, path: String, rule: TextFormatRule) {
        val sub = textSec.createSection(path)
        sub.set(keys.textFont, rule.font)
        if (rule.prefix.isNotEmpty()) sub.set(keys.textPrefix, rule.prefix)
        if (rule.suffix.isNotEmpty()) sub.set(keys.textSuffix, rule.suffix)
        if (rule.wrapLength != -1) sub.set(keys.textWrapLength, rule.wrapLength)
        if (rule.weights.isNotEmpty()) sub.set(keys.textWeight, rule.weights)
        if (rule.textCase != net.justlime.limeframegui.enums.TextCase.REGULAR) {
            sub.set(keys.textCase, rule.textCase.name.lowercase().replace("_", "-"))
        }
    }

    private fun guiTextSettings(section: ConfigurationSection): GuiTextSettings = GuiTextSettings(
        title = section.getConfigurationSection(keys.textSection)?.let { textSec ->
            val rule = TextFormatRule(font = keys.stylishTitle)
            TemplateCompiler.parseTextGroupRule(textSec, "title", rule, keys)
            rule
        } ?: GuiTextSettings().title,
        name = section.getConfigurationSection(keys.textSection)?.let { textSec ->
            val rule = TextFormatRule(font = keys.stylishName)
            TemplateCompiler.parseTextGroupRule(textSec, "name", rule, keys)
            rule
        } ?: GuiTextSettings().name,
        lore = section.getConfigurationSection(keys.textSection)?.let { textSec ->
            val rule = TextFormatRule(font = keys.stylishLore)
            TemplateCompiler.parseTextGroupRule(textSec, "lore", rule, keys)
            rule
        } ?: GuiTextSettings().lore
    )

}