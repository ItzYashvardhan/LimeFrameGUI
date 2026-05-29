package net.justlime.limeframegui.config

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.models.*
import net.justlime.limeframegui.registry.common.TemplateCompiler
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.util.FrameConverter
import net.justlime.limeframegui.util.toGuiItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

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
        // 1. Resolve Audio and Action Metadata
        val soundAlias = section.getString(keys.stylishItemSound)
            ?: section.getStringList(keys.stylishItemSound).firstOrNull()
        val parsedAction = section.getString(keys.action) ?: ActionRegistry.registerInline(section)

        // 2. Construct the Base Visual ItemStack
        val material = Material.getMaterial(section.getString(keys.material) ?: "AIR") ?: Material.AIR
        val amount = section.getInt(keys.amount, 1)
        val baseItem = ItemStack(material, amount)

        val meta = baseItem.itemMeta
        if (meta != null) {
            if (section.getBoolean(keys.unbreakable, false)) {
                meta.isUnbreakable = true
            }

            if (section.contains(keys.damage) && meta is Damageable) {
                meta.damage = section.getInt(keys.damage)
            }

            if (section.contains(keys.model)) {
                meta.setCustomModelData(section.getInt(keys.model))
            }

            val flags =
                section.getStringList(keys.flags).mapNotNull { runCatching { ItemFlag.valueOf(it) }.getOrNull() }
            if (flags.isNotEmpty()) {
                meta.addItemFlags(*flags.toTypedArray())
            }

            if (section.getBoolean(keys.glow, false)) {
                try {
                    meta.setEnchantmentGlintOverride(true) // 1.20.4+ Native Glow
                } catch (_: NoSuchMethodError) {
                    // Legacy Fallback
                    val dummyEnchant = Enchantment.getByName("UNBREAKING") ?: Enchantment.getByName("DURABILITY")
                    if (dummyEnchant != null) {
                        meta.addEnchant(dummyEnchant, 1, true)
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }
            baseItem.itemMeta = meta
        }

        // 3. Parse Data-Driven Session States (Recursive)
        val stateId = section.getString(keys.stateId)
        val states = mutableMapOf<String, GuiItem>()
        val statesSec = section.getConfigurationSection(keys.states)
        if (statesSec != null) {
            for (stateKey in statesSec.getKeys(false)) {
                val stateSection = statesSec.getConfigurationSection(stateKey)
                if (stateSection != null) {
                    states[stateKey.uppercase()] = loadItem(stateSection)
                }
            }
        }

        // 4. Assemble the Blueprint
        return GuiItem(
            baseItem = baseItem,
            name = section.getString(keys.name) ?: "",
            lore = section.getStringList(keys.lore),
            viewRequirements = section.getStringList(keys.viewRequirements),
            priority = section.getInt(keys.priority, 0),
            slot = section.getString(keys.slot)?.toIntOrNull(),
            slotList = section.getIntegerList(keys.slotList),
            updateInterval = section.getString(keys.updateInterval)?.toIntOrNull(),
            texture = section.getString(keys.texture),
            stateId = stateId,
            states = states,
            style = GuiStyleSheet(
                textSettings = GuiTextSettings(
                    name = section.getConfigurationSection(keys.textSection)?.let { textSec ->
                        val rule = TextFormatRule()
                        TemplateCompiler.parseTextGroupRule(textSec, "name", rule, keys)
                        rule
                    } ?: TextFormatRule(),
                    lore = section.getConfigurationSection(keys.textSection)?.let { textSec ->
                        val rule = TextFormatRule()
                        TemplateCompiler.parseTextGroupRule(textSec, "lore", rule, keys)
                        rule
                    } ?: TextFormatRule()
                ),
                clickSoundAlias = soundAlias,
                action = parsedAction
            )
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
                // Just use the base visual item for a static inventory load
                inventory.setItem(slot, item.baseItem.clone())
            }
        }
        return inventory
    }

    /**
     * Formats settings required to build context-aware, text-input Anvil UI panels.
     */
    fun loadAnvilSetting(section: ConfigurationSection?): AnvilGuiSetting {
        val keys = LimeFrameAPI.keys
        if (section == null) return AnvilGuiSetting(
            title = keys.defaultAnvilTitle,
            label = keys.defaultAnvilLabel,
            leftItem = GuiItem(),
            rightItem = GuiItem(),
            outPutItem = GuiItem(),
            style = GuiStyleSheet()
        )

        val leftItem = section.getConfigurationSection(keys.anvilLeftItem)?.let { loadItem(it) } ?: GuiItem()
        val rightItem = section.getConfigurationSection(keys.anvilRightItem)?.let { loadItem(it) } ?: GuiItem()
        val outputItem = section.getConfigurationSection(keys.anvilOutputItem)?.let { loadItem(it) } ?: GuiItem()

        val mainSec = section.getConfigurationSection(keys.main)

        return AnvilGuiSetting(
            title = mainSec?.getString(keys.anvilTitle, keys.defaultAnvilTitle) ?: keys.defaultAnvilTitle,
            label = mainSec?.getString(keys.anvilLabel, keys.defaultAnvilLabel) ?: keys.defaultAnvilLabel,
            preventClose = mainSec?.getBoolean(keys.anvilPreventClose, false) ?: false,
            leftItem = leftItem,
            rightItem = rightItem,
            outPutItem = outputItem,

            // Sounds are typically at the root or main depending on your config, this checks the root!
            openSoundAlias = section.getString(keys.stylishOpenSound) ?: section.getStringList(keys.stylishOpenSound)
                .firstOrNull(),
            cancelSoundAlias = section.getString(keys.anvilCancelSound) ?: section.getStringList(keys.anvilCancelSound)
                .firstOrNull(),
            submitSoundAlias = section.getString(keys.anvilSubmitSound) ?: section.getStringList(keys.anvilSubmitSound)
                .firstOrNull(),

            style = GuiStyleSheet()
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
        val base = item.baseItem
        val meta = base.itemMeta

        // 1. Write the Raw Text Templates
        if (item.name.isNotEmpty()) section.set(keys.name, item.name)
        if (item.lore.isNotEmpty()) section.set(keys.lore, item.lore)

        // 2. Write the Visual ItemStack Properties
        section.set(keys.material, base.type.name)
        section.set(keys.amount, base.amount)

        if (meta != null) {
            if (meta.hasItemFlag(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)) {
                section.set(keys.glow, true)
            } else if (meta.hasEnchants()) {
                // Support for the modern API if you use it instead of HIDE_ENCHANTS
                try {
                    if (meta.hasEnchantmentGlintOverride()) {
                        section.set(keys.glow, meta.enchantmentGlintOverride)
                    }
                } catch (_: NoSuchMethodError) {
                }
            }

            if (meta.itemFlags.isNotEmpty()) {
                section.set(keys.flags, meta.itemFlags.map { it.name })
            }

            if (meta.hasCustomModelData()) {
                section.set(keys.model, meta.customModelData)
            }

            if (meta.isUnbreakable) {
                section.set(keys.unbreakable, true)
            }

            if (meta is Damageable && meta.hasDamage()) {
                section.set(keys.damage, meta.damage)
            }
        }

        // 3. Write LimeFrame Properties
        if (item.texture != null) section.set(keys.texture, item.texture)
        if (item.updateInterval != null) section.set(keys.updateInterval, item.updateInterval)

        item.slot?.let { section.set(keys.slot, it) }
        if (item.slotList.isNotEmpty()) section.set(keys.slotList, item.slotList)

        if (item.viewRequirements.isNotEmpty()) section.set(LimeFrameAPI.keys.viewRequirements, item.viewRequirements)
        if (item.priority != 0) section.set("priority", item.priority)

        // 4. Write Stylish Settings
        item.style.clickSoundAlias?.let { section.set(keys.stylishItemSound, it) }

        val textSec = section.createSection(keys.textSection)
        writeRule(textSec, "name", item.style.textSettings.name)
        writeRule(textSec, "lore", item.style.textSettings.lore)
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
        if (rule.prefix?.isNotEmpty() == true) sub.set(keys.textPrefix, rule.prefix)
        if (rule.suffix?.isNotEmpty() == true) sub.set(keys.textSuffix, rule.suffix)
        if (rule.wrapLength != -1) sub.set(keys.textWrapLength, rule.wrapLength)
        if (rule.weights?.isNotEmpty() == true) sub.set(keys.textWeight, rule.weights)
        if (rule.textCase != net.justlime.limeframegui.enums.TextCase.REGULAR) {
            sub.set(keys.textCase, rule.textCase?.name?.lowercase()?.replace("_", "-"))
        }
    }

    private fun guiTextSettings(section: ConfigurationSection): GuiTextSettings = GuiTextSettings(
        title = section.getConfigurationSection(keys.textSection)?.let { textSec ->
            val rule = TextFormatRule(font = keys.stylishTitle)
            TemplateCompiler.parseTextGroupRule(textSec, "title", rule, keys)
            rule
        } ?: TextFormatRule(font = keys.stylishTitle),

        name = section.getConfigurationSection(keys.textSection)?.let { textSec ->
            val rule = TextFormatRule(font = keys.stylishName)
            TemplateCompiler.parseTextGroupRule(textSec, "name", rule, keys)
            rule
        } ?: TextFormatRule(font = keys.stylishName),
        lore = section.getConfigurationSection(keys.textSection)?.let { textSec ->
            val rule = TextFormatRule(font = keys.stylishLore)
            TemplateCompiler.parseTextGroupRule(textSec, "lore", rule, keys)
            rule
        } ?: TextFormatRule(font = keys.stylishLore)
    )

}