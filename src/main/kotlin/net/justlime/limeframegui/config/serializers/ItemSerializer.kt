package net.justlime.limeframegui.config.serializers

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.config.FrameKeys
import net.justlime.limeframegui.config.GuiDirectoryHandler
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.GuiTextSettings
import net.justlime.limeframegui.models.TextFormatRule
import net.justlime.limeframegui.registry.common.TemplateCompiler
import net.justlime.limeframegui.registry.component.ActionRegistry
import net.justlime.limeframegui.registry.component.LangRegistry
import net.justlime.limeframegui.registry.component.PlaceholderRegistry
import net.justlime.limeframegui.registry.component.StateRegistry
import net.justlime.limeframegui.util.actionString
import net.justlime.limeframegui.util.componentKey
import net.justlime.limeframegui.util.explicitDisplay
import net.justlime.limeframegui.util.explicitLore
import net.justlime.limeframegui.util.explicitName
import net.justlime.limeframegui.util.isGlowing
import net.justlime.limeframegui.util.isUnbreakable
import net.justlime.limeframegui.util.itemClickSound
import net.justlime.limeframegui.util.materialStr
import net.justlime.limeframegui.util.slotIndex
import net.justlime.limeframegui.util.slotListIndices
import net.justlime.limeframegui.util.stateIdStr
import net.justlime.limeframegui.util.updateIntervalTick
import net.justlime.limeframegui.util.viewReqs
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

object ItemSerializer {

    fun loadItem(section: ConfigurationSection): GuiItem {
        val soundAlias = section.itemClickSound
        val parsedAction = section.actionString ?: ActionRegistry.registerInline(section)

        // Fetch Component
        val componentKey = section.componentKey
        val sharedComponent = if (!componentKey.isNullOrBlank()) StateRegistry.get(componentKey) else null

        if (componentKey != null && sharedComponent == null) {
            GuiDirectoryHandler.plugin.logger.warning("[LimeFrameGUI] Component '$componentKey' referenced at ${section.currentPath} could not be found in StateRegistry.")
        }

        // DISPLAY MAPPING LOGIC
        val itemKey = section.name
        val explicitName = section.explicitName
        val explicitLore = section.explicitLore
        val explicitDisplay = section.explicitDisplay

        var finalName = sharedComponent?.currentName ?: ""
        var finalLore = sharedComponent?.currentLore ?: emptyList()

        when {
            explicitName != null || explicitLore.isNotEmpty() -> {
                finalName = explicitName ?: finalName
                finalLore = explicitLore.ifEmpty { finalLore }
            }
            explicitDisplay != null -> {
                val rawLangKey = explicitDisplay.removePrefix("lang.").removePrefix("lang:")
                val langList = LangRegistry.getList(rawLangKey)
                if (langList.isNotEmpty()) {
                    finalName = langList.first()
                    finalLore = langList.drop(1)
                }
            }
            else -> {
                if (finalName.isEmpty() && finalLore.isEmpty()) {
                    val langList = LangRegistry.getList(itemKey)
                    if (langList.isNotEmpty()) {
                        finalName = langList.first()
                        finalLore = langList.drop(1)
                    }
                }
            }
        }

        // MATERIAL LOGIC
        val localMaterial = section.materialStr
        var materialString = sharedComponent?.baseItemString ?: ""
        var material: Material = sharedComponent?.baseItem?.type ?: Material.STONE

        if (localMaterial != null) {
            val isPlaceHolderAPISyntax = localMaterial.contains("%")
            val isLocalPlaceholderSyntax = localMaterial.contains(PlaceholderRegistry.prefix) && localMaterial.contains(
                PlaceholderRegistry.suffix)

            if (isPlaceHolderAPISyntax || isLocalPlaceholderSyntax) {
                materialString = localMaterial
            } else {
                materialString = ""
                val matched = Material.matchMaterial(localMaterial.uppercase())
                if (matched != null) {
                    material = matched
                } else {
                    GuiDirectoryHandler.plugin.logger.warning("[LimeFrameGUI] Invalid material '${localMaterial}' at ${section.currentPath}.")
                    material = Material.STONE
                }
            }
        }

        val amount = section.getInt(FrameKeys.Item.AMOUNT, sharedComponent?.baseItem?.amount ?: 1)
        val baseItem = if (localMaterial == null && sharedComponent != null) {
            sharedComponent.baseItem.clone().apply { this.amount = amount }
        } else {
            ItemStack(material, amount)
        }
        val meta = baseItem.itemMeta
        if (meta != null) {
            if (section.isUnbreakable) meta.isUnbreakable = true
            if (section.contains(FrameKeys.Item.DAMAGE) && meta is Damageable) meta.damage = section.getInt(FrameKeys.Item.DAMAGE)
            if (section.contains(FrameKeys.Item.MODEL)) meta.setCustomModelData(section.getInt(FrameKeys.Item.MODEL))

            val flags = section.getStringList(FrameKeys.Item.FLAGS).mapNotNull { runCatching { ItemFlag.valueOf(it) }.getOrNull() }
            if (flags.isNotEmpty()) meta.addItemFlags(*flags.toTypedArray())

            if (section.isGlowing) {
                try { meta.setEnchantmentGlintOverride(true) } catch (_: NoSuchMethodError) {
                    val dummyEnchant = Enchantment.getByName("UNBREAKING") ?: Enchantment.getByName("DURABILITY")
                    if (dummyEnchant != null) {
                        meta.addEnchant(dummyEnchant, 1, true)
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                    }
                }
            }
            baseItem.itemMeta = meta
        }

        // STATE PARSING
        val stateId = section.stateIdStr ?: sharedComponent?.stateId
        val states = mutableMapOf<String, GuiItem>()

        sharedComponent?.states?.forEach { (k, v) -> states[k] = v.clone() }

        val statesSec = section.getConfigurationSection(FrameKeys.State.STATES)
        if (statesSec != null) {
            for (stateKey in statesSec.getKeys(false)) {
                val stateSection = statesSec.getConfigurationSection(stateKey)
                if (stateSection != null) {
                    states[stateKey.uppercase()] = loadItem(stateSection)
                }
            }
        }

        // ASSEMBLE BLUEPRINT
        return GuiItem(
            baseItem = baseItem,
            baseItemString = materialString,
            name = finalName,
            lore = finalLore,
            viewRequirements = section.viewReqs,
            priority = section.getInt(FrameKeys.Item.PRIORITY, sharedComponent?.priority ?: 0),
            slot = section.slotIndex ?: sharedComponent?.slot,
            slotList = section.slotListIndices.takeIf { it.isNotEmpty() } ?: sharedComponent?.slotList ?: emptyList(),
            updateInterval = section.updateIntervalTick ?: sharedComponent?.updateInterval,
            texture = section.getString(FrameKeys.Item.TEXTURE) ?: sharedComponent?.texture,
            stateId = stateId,
            states = states,
            style = GuiStyleSheet(
                textSettings = GuiTextSettings(
                    name = section.getConfigurationSection(FrameKeys.Text.SECTION)?.let { textSec ->
                        val rule = TextFormatRule()
                        TemplateCompiler.parseTextGroupRule(textSec, "name", rule)
                        rule
                    } ?: sharedComponent?.style?.textSettings?.name ?: TextFormatRule(),
                    lore = section.getConfigurationSection(FrameKeys.Text.SECTION)?.let { textSec ->
                        val rule = TextFormatRule()
                        TemplateCompiler.parseTextGroupRule(textSec, "lore", rule)
                        rule
                    } ?: sharedComponent?.style?.textSettings?.lore ?: TextFormatRule()
                ),
                clickSoundAlias = soundAlias ?: sharedComponent?.style?.clickSoundAlias,
                action = parsedAction.takeIf { it?.isNotBlank() == true } ?: sharedComponent?.style?.action ?: ""
            )
        )
    }

    fun loadItems(section: ConfigurationSection): List<GuiItem> {
        return section.getKeys(false).mapNotNull { key ->
            section.getConfigurationSection(key)?.let { loadItem(it) }
        }
    }
}