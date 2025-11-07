package net.justlime.limeframegui.models

import net.justlime.limeframegui.color.FontColor
import net.justlime.limeframegui.utilities.SkullProfileCache
import net.justlime.limeframegui.utilities.SkullUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

/**
 * Represents an item in a LimeFrame GUI.
 *
 * @param material The Bukkit material type of the item.
 * @param name The display name of the item.
 * @param amount The number of items in the stack (default: 1).
 * @param lore A list of lore lines displayed under the item name.
 * @param glow Whether the item should visually glow.
 * @param flags Item flags to hide or show specific properties in tooltips.
 * @param customModelData Custom model data ID for resource pack integration.
 * @param texture Base64-encoded texture string for custom player heads (only works with PLAYER_HEAD).
 * @param enchantments Map of enchantments and their levels for this item.
 * @param unbreakable Whether the item is unbreakable (no durability loss).
 * @param damage The current damage/durability value of the item (0 = new).
 * @param slot Single slot index to place this item into.
 * @param slotList Multiple slot indices to place the same item in several slots.
 * @param placeholderPlayer Player object used for applying placeholders in displayName and lore.
 * @param placeholderOfflinePlayer OfflinePlayer object used for applying placeholders in displayName and lore.
 * @param smallCapsName Whether to apply small caps formatting to the display name.
 * @param smallCapsLore Whether to apply small caps formatting to the lore.
 * @param onClick Event callback invoked when this item is clicked in the GUI (TODO).
 */
data class GuiItem(

    // Appearance
    var material: Material = Material.AIR,
    var name: String = "",
    var lore: List<String> = mutableListOf(),
    var amount: Int = 1,
    var glow: Boolean = false,
    var flags: List<ItemFlag> = emptyList(),
    var customModelData: Int? = null,
    var texture: String? = null,

    // Functional Meta
    var enchantments: Map<Enchantment, Int> = emptyMap(),
    var unbreakable: Boolean = false,
    var damage: Int? = null, // Durability value

    // Placement
    var slot: Int? = null,
    var slotList: List<Int> = mutableListOf(),

    // Placeholder & Dynamic Content
    val nameState: (() -> String)? = null,
    val loreState: (() -> List<String>)? = null,
    var customPlaceholder: Map<String, String>? = null,
    var placeholderPlayer: Player? = null,
    var placeholderOfflinePlayer: OfflinePlayer? = null,
    var smallCapsName: Boolean? = null,
    var smallCapsLore: Boolean? = null,

    //base ItemStack
    var itemStack: ItemStack? = null,

    // Click Handling
    var onClick: (InventoryClickEvent) -> Unit = {}
) {
    init {
        this.itemStack = this.toItemStack()
    }

    val currentName: String
        get() = nameState?.invoke() ?: name

    val currentLore: List<String>
        get() = loreState?.invoke() ?: lore

    companion object {
        fun air(): GuiItem {
            return GuiItem(name = "", material = Material.AIR)
        }
    }

    fun toItemStack(): ItemStack {
        val item = this.itemStack?.clone()?.apply {
            if (this@GuiItem.amount != 1 || this.amount <= 0) {
                this.amount = this@GuiItem.amount
            }
        } ?: if (material.name.contains("PLAYER_HEAD", ignoreCase = true) && !texture.isNullOrEmpty()) {
            ItemStack(Material.PLAYER_HEAD, amount)
        } else {
            // If no items tack is provided, ensure material is not AIR (unless intended)
            if (material == Material.AIR && name.isEmpty() && currentName.isEmpty() && lore.isEmpty() && currentLore.isEmpty()) {
                return ItemStack(Material.AIR)
            }
            try {
                ItemStack(material, amount)
            } catch (e: Exception) {
                ItemStack(Material.AIR)
            }
        }

        val meta = item.itemMeta ?: return item

        //Skull Texture
        if (meta is SkullMeta && !texture.isNullOrEmpty()) {
            if (texture.equals("{player}", ignoreCase = true)) {

                if (placeholderPlayer != null) {
                    if (SkullUtils.VersionHelper.HAS_PLAYER_PROFILES) {
                        // Modern Way (1.18+)
                        meta.ownerProfile = placeholderPlayer!!.playerProfile
                    } else {
                        // Legacy Way (pre-1.18)
                        meta.owningPlayer = placeholderPlayer
                    }
                } else if (placeholderOfflinePlayer != null) {
                    meta.owningPlayer = placeholderOfflinePlayer
                }

            } else if (texture!!.startsWith("[") && texture!!.endsWith("]")) {
                // It's a UUID, e.g., "[069a79f4-44e9-4726-a5be-fca90e38aaf5]"
                try {
                    val uuidString = texture!!.substring(1, texture!!.length - 1)
                    val uuid = UUID.fromString(uuidString)
                    val owner = Bukkit.getOfflinePlayer(uuid)
                    if (SkullUtils.VersionHelper.HAS_PLAYER_PROFILES) {
                        meta.ownerProfile = owner.playerProfile
                    } else {
                        meta.owningPlayer = owner
                    }
                } catch (_: IllegalArgumentException) {
                    // Ignore if the UUID is malformed. The skull will be default.
                }
            } else {
                // Assume it's a Base64 texture value
                SkullUtils.applySkin(meta, SkullProfileCache.getProfile(texture!!))
            }
        }

        meta.setDisplayName(FontColor.applyColor(if (nameState != null) currentName else name, placeholderPlayer, placeholderOfflinePlayer, smallCapsName, customPlaceholder))
        if (lore.isNotEmpty() || currentLore.isNotEmpty()) {
            meta.lore = try {
                FontColor.applyColor(if (loreState != null) currentLore else lore, placeholderPlayer, placeholderOfflinePlayer, smallCapsLore, customPlaceholder)
            } catch (_: Throwable) {
                lore
            }
        }

        // Glow
        try {
            meta.setEnchantmentGlintOverride(glow)
        } catch (_: Throwable) {
            if (glow) {
                val unbreaking = Enchantment.getByName("UNBREAKING") ?: Enchantment.getByName("DURABILITY")!!
                meta.addEnchant(unbreaking, 1, true)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        }

        // Item flags
        if (flags.isNotEmpty()) {
            try {
                meta.addItemFlags(*flags.toTypedArray())
            } catch (_: Throwable) {
                // Ignore
            }
        }

        // Custom model data
        customModelData?.let {
            try {
                meta.setCustomModelData(it)
            } catch (_: Throwable) {
                // Ignore
            }
        }

        // Enchantments
        if (enchantments.isNotEmpty()) {
            enchantments.forEach { (enchantment, lvl) -> meta.addEnchant(enchantment, lvl, true) }
        }

        // Unbreakable
        try {
            meta.isUnbreakable = unbreakable
        } catch (_: Throwable) {
            // Ignore
        }

        // Damage (durability)
        damage?.let {
            if (meta is Damageable) {
                try {
                    meta.damage = it
                } catch (_: Throwable) {
                    // Ignore
                }
            }
        }

        item.itemMeta = meta
        return item
    }

    /**
     * Creates a deep copy of the GuiItem.
     */
    fun clone(): GuiItem {
        return this.copy(
            lore = this.lore.toMutableList(), flags = this.flags.toList(), slotList = this.slotList.toMutableList(), enchantments = this.enchantments.toMap(), customPlaceholder = this.customPlaceholder?.toMap(), itemStack = this.itemStack?.clone()
        )
    }
}
