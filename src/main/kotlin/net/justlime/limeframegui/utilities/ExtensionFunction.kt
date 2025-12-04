package net.justlime.limeframegui.utilities

import net.justlime.limeframegui.models.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

// Temporary storage for attaching a GuiItem to an InventoryClickEvent.
private val clickEventItems = WeakHashMap<InventoryClickEvent, GuiItem?>()

/**
 * Converts an [ItemStack] to a [GuiItem].
 *
 * This extension function extracts relevant properties from an [ItemStack] and its [ItemMeta]
 * to create a [GuiItem] object. It handles display name, lore, glow effect, item flags,
 * custom model data, and skull textures for player heads.
 *
 * @return A [GuiItem] representation of the [ItemStack].
 */
fun ItemStack.toGuiItem(): GuiItem {
    val meta = this.itemMeta

    val displayName = meta?.displayName ?: this.type.name
    val lore = meta?.lore ?: emptyList()

    val glow = try {
        if (meta?.hasEnchantmentGlintOverride() == true) {
            meta.enchantmentGlintOverride
        } else {
            meta?.hasEnchants() == true // fallback for old versions
        }
    } catch (_: Throwable) {
        meta?.hasEnchants() == true // 1.8.8 fallback
    }

    val flags = meta?.itemFlags?.map { it } // store as string for compatibility
        ?: emptyList()

    val customModelData = try {
        if (meta?.hasCustomModelData() == true) meta.customModelData else null
    } catch (_: Throwable) {
        null
    }

    // Skull texture
    var skullTexture: String? = null
    if (this.type.name.contains("PLAYER_HEAD", ignoreCase = true) && meta is SkullMeta) {
        skullTexture = SkullUtils.getTextureFromSkull(this)
    }

    // Enchantments
    val enchantments = meta?.enchants ?: emptyMap()

    // Unbreakable
    val unbreakable = try {
        meta?.isUnbreakable == true
    } catch (_: Throwable) {
        false
    }

    // Damage
    val damage = try {
        if (meta is Damageable) {
            meta.damage
        } else null
    } catch (_: Throwable) {
        null
    }

    // Hide tooltip
    val hideToolTip = try {
        meta?.isHideTooltip == true
    } catch (_: Throwable) {
        false
    }

    // Attribute modifiers
    val attributeModifiers = try {
        meta?.attributeModifiers
    } catch (_: Throwable) {
        null
    }

    return GuiItem(
        material = type, name = displayName, amount = amount, lore = lore.toMutableList(), glow = glow, flags = flags, customModelData = customModelData, texture = skullTexture, enchantments = enchantments, unbreakable = unbreakable, damage = damage
    )
}

/**
 * Adds the given GuiItem to the inventory, returning any items that could not be added.
 *
 * @param item The GuiItem to add. If null, nothing is added.
 * @return A HashMap where keys are the slot indices and values are the ItemStacks that could not be added (e.g., inventory full).
 */
fun Inventory.addItem(item: GuiItem?): HashMap<Int, ItemStack> {
    if (item == null) return hashMapOf()
    val remaining = this.addItem(item.toItemStack()) // returns Map<Int, ItemStack>
    return remaining.ifEmpty { hashMapOf() }
}

/**
 * Adds the given GuiItem to the inventory, returning any items that could not be added.
 *
 * @param item The GuiItem to add. If null, nothing is added.
 * @return A HashMap where keys are the slot indices and values are the ItemStacks that could not be added (e.g., inventory full).
 */
fun Inventory.addItems(item: List<GuiItem>): List<HashMap<Int, ItemStack>> {
    val remainingItems = mutableListOf<HashMap<Int, ItemStack>>()
    item.forEach { guiItem ->
        val remaining = this.addItem(guiItem.toItemStack())
        if (remaining.isNotEmpty()) {
            remainingItems.add(remaining)
        }
    }
    return remainingItems
}

/**
 * Sets the item at the specified slot in the inventory.
 *
 * @param index The slot index where the item should be placed.
 * @param item The GuiItem to set. If null, the slot will be cleared.
 * @return True if the item was successfully set, false otherwise (e.g., invalid index).
 */
fun Inventory.setItem(index: Int, item: GuiItem?): Boolean {
    if (item == null) {
        this.setItem(index, null)
        return true
    }
    val stack = item.toItemStack()

    if (index in 0 until this.size) {
        this.setItem(index, stack)
        return true
    }
    return false
}

/**
 * Sets the item at the specified slots in the inventory.
 *
 * @param index A list of slot indices where the item should be placed.
 * @param item The GuiItem to set. If null, the slots will be cleared.
 * @return A list of slot indices where the item could not be set (e.g., invalid index).
 */
fun Inventory.setItem(index: List<Int>, item: GuiItem?): List<Int> {
    val failedSlots = mutableListOf<Int>()
    for (i in index) {
        if (!setItem(i, item)) {
            failedSlots.add(i)
        }
    }
    return failedSlots
}

/**

 * Removes the item at the specified slot in the inventory.
 *
 * @param slot The slot index of the item to remove.
 * @return True if the item was successfully removed, false otherwise (e.g., invalid index).
 */
fun Inventory.remove(slot: Int): Boolean {
    if (slot >= 0 && slot < this.size) {
        this.setItem(slot, null)
        return true
    }
    return false
}

/**
 * Removes the items at the specified slots in the inventory.
 *
 * @param slot A list of slot indices of the items to remove.
 * @return A list of slot indices where the item could not be removed (e.g., invalid index).
 */
fun Inventory.remove(slot: List<Int>): List<Int> {
    val failedSlots = mutableListOf<Int>()
    for (i in slot) {
        if (!remove(i)) {
            failedSlots.add(i)
        }
    }
    return failedSlots
}

/**
 * * Removes the given GuiItem from the inventory.
 *
 * @param item The GuiItem to remove.
 * @return True if the item was successfully removed, false otherwise.
 */
fun Inventory.remove(item: GuiItem): Boolean {
    val stackToRemove = item.toItemStack()
    val result = this.removeItem(stackToRemove)
    return result.isEmpty() // If nothing is left, it means removal was successful

}

fun Pair<Int, Int>.toSlot(totalRows: Int = 6): Int {
    var (row, col) = this

    if (row == 0 && col == 0) return -1

    row = if (row < 0) totalRows + row + 1 else row
    col = if (col < 0) 9 + col + 1 else col

    return (row - 1) * 9 + (col - 1)
}

/**
 * The GuiItem associated with this click event.
 *
 * This property is set by the GUI framework **before** your click handler runs.
 * It allows you to directly access the clicked GuiItem in your event handler:
 *
 * **Note:** This is not persisted outside the event call.
 * Once the event finishes, the reference will be eligible for Garbage Collection.
 */
var InventoryClickEvent.item: GuiItem?
    get() = clickEventItems[this]
    set(value) {
        clickEventItems[this] = value
    }

/**
 * Updates the item in the inventory at the clicked slot with the current state of the `item` property
 * of this [InventoryClickEvent].
 */
fun InventoryClickEvent.update() {
    this.inventory.setItem(this.slot, this.item)
}
