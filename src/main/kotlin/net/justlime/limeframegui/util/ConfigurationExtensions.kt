package net.justlime.limeframegui.util

import net.justlime.limeframegui.config.FrameKeys
import org.bukkit.configuration.ConfigurationSection

// ==========================================
// ITEM EXTENSIONS
// ==========================================

val ConfigurationSection.componentKey: String?
    get() = getString(FrameKeys.Item.COMPONENT)

val ConfigurationSection.materialStr: String?
    get() = getString(FrameKeys.Item.MATERIAL)

val ConfigurationSection.explicitName: String?
    get() = getString(FrameKeys.Item.NAME)

val ConfigurationSection.explicitDisplay: String?
    get() = getString(FrameKeys.Item.DISPLAY)

/** Safely handles lore whether the user wrote a single string or a list in YAML */
val ConfigurationSection.explicitLore: List<String>
    get() = if (isList(FrameKeys.Item.LORE)) {
        getStringList(FrameKeys.Item.LORE)
    } else {
        getString(FrameKeys.Item.LORE)?.let { listOf(it) } ?: emptyList()
    }

val ConfigurationSection.itemAmount: Int
    get() = getInt(FrameKeys.Item.AMOUNT, 1)

val ConfigurationSection.slotIndex: Int?
    get() = getString(FrameKeys.Item.SLOT)?.toIntOrNull()

val ConfigurationSection.slotListIndices: List<Int>
    get() = getIntegerList(FrameKeys.Item.SLOTS).ifEmpty {
        getIntegerList(FrameKeys.Item.SLOT)
    }
val ConfigurationSection.updateIntervalTick: Int?
    get() = getString(FrameKeys.Item.UPDATE_INTERVAL)?.toIntOrNull()

val ConfigurationSection.viewReqs: List<String>
    get() = getStringList(FrameKeys.Item.VIEW_REQUIREMENT)

// ==========================================
// STYLE & META EXTENSIONS
// ==========================================

val ConfigurationSection.actionString: String?
    get() = getString(FrameKeys.Item.ACTION)

val ConfigurationSection.itemClickSound: String?
    get() = getString(FrameKeys.Sound.SOUND_CLICK)
        ?: getStringList(FrameKeys.Sound.SOUND_CLICK).firstOrNull()

val ConfigurationSection.isUnbreakable: Boolean
    get() = getBoolean(FrameKeys.Item.UNBREAKABLE, false)

val ConfigurationSection.isGlowing: Boolean
    get() = getBoolean(FrameKeys.Item.GLOW, false)

// ==========================================
// STATE EXTENSIONS
// ==========================================

val ConfigurationSection.stateIdStr: String?
    get() = getString(FrameKeys.State.STATE_ID)