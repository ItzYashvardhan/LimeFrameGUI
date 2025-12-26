package net.justlime.limeframegui.models

import net.justlime.limeframegui.color.FontStyle
import net.justlime.limeframegui.integration.SkinRestorerHook
import net.justlime.limeframegui.utilities.SkullProfileCache
import net.justlime.limeframegui.utilities.SkullUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
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
 * @param style StyleSheet to display stylish and dynamic text (i.e, custom placeholder, small cap's font)
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
    var style: GuiStyleSheet = GuiStyleSheet(),

    private var baseItemStack: ItemStack? = null,

    // Click Handling
    var onClick: (InventoryClickEvent) -> Unit = {}
) {

    // Helper properties to resolve dynamic state vs static state
    val currentName: String get() = nameState?.invoke() ?: name

    val currentLore: List<String> get() = loreState?.invoke() ?: lore

    /**
     * Renders the GuiItem into a Bukkit ItemStack.
     * This merges the 'baseItemStack' (if present) with the specific properties defined in this class.
     */
    fun toItemStack(): ItemStack {
        // 1. Determine the starting ItemStack
        val item = if (baseItemStack != null) {
            baseItemStack!!.clone()
        } else {

            // Handle Special Case: Textured Player Heads
            if (isPlayerHead() && !texture.isNullOrEmpty()) {
                ItemStack(Material.PLAYER_HEAD)
            } else {
                // If material is AIR and no content exists, return AIR immediately
                if (material == Material.AIR && isEmpty()) return ItemStack(Material.AIR)
                ItemStack(material)
            }
        }

        // 2. Update Amount
        item.amount = amount.coerceAtLeast(1)

        val meta = item.itemMeta ?: return item

        // 3. Apply Textures (If Skull)
        if (meta is SkullMeta && !texture.isNullOrEmpty()) {
            applySkullTexture(meta)
        }

        // 4. Apply Display Name & Lore (with placeholders and colors)
        style.let {
            val finalName = FontStyle.applyStyle(currentName, it, it.stylishName)
            meta.setDisplayName(finalName)
        }
        style.let {
            val rawLore = currentLore
            if (rawLore.isNotEmpty()) {
                meta.lore = FontStyle.applyStyle(rawLore, it, it.stylishLore)
            }
        }

        // 5. Apply Glow
        if (glow) {
            try {
                // Modern API (1.20.4+)
                meta.setEnchantmentGlintOverride(true)
            } catch (_: NoSuchMethodError) {
                // Legacy Fallback
                val dummyEnchant = Enchantment.getByName("UNBREAKING") ?: Enchantment.getByName("DURABILITY")
                if (dummyEnchant != null) {
                    meta.addEnchant(dummyEnchant, 1, true)
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                }
            }
        }

        // 6. Apply Item Flags
        if (flags.isNotEmpty()) {
            meta.addItemFlags(*flags.toTypedArray())
        }

        // 7. Custom Model Data
        if (customModelData != null) {
            meta.setCustomModelData(customModelData)
        }

        // 8. Enchantments
        if (enchantments.isNotEmpty()) {
            enchantments.forEach { (enchantment, lvl) -> meta.addEnchant(enchantment, lvl, true) }
        }

        // 9. Unbreakable & Damage
        meta.isUnbreakable = unbreakable
        if (damage != null && meta is Damageable) {
            meta.damage = damage!!
        }

        item.itemMeta = meta
        return item
    }

    /**
     * Updates the internal base item stack.
     */
    fun setItemStack(stack: ItemStack) {
        this.baseItemStack = stack.clone()
        this.material = stack.type
        this.amount = stack.amount
    }

    fun getItemStack(): ItemStack? = baseItemStack

    /**
     * Creates a deep copy of the GuiItem.
     */
    fun clone(): GuiItem {
        return this.copy(
            lore = ArrayList(this.lore), flags = ArrayList(this.flags), slotList = ArrayList(this.slotList), enchantments = HashMap(this.enchantments), style = this.style.copy(), baseItemStack = this.baseItemStack?.clone()
        )
    }


    fun applyStyleSheet(style: GuiStyleSheet) {
        this.style.let {
            it.viewer = style.viewer
            it.offlinePlayer = style.offlinePlayer
            it.placeholder = style.placeholder
            it.openSound = style.openSound
            it.closeSound = style.closeSound
            it.clickSound = style.clickSound
        }
    }


    /**
     * Applies texture logic specifically for SkullMeta.
     */
    private fun applySkullTexture(meta: SkullMeta) {
        val tex = texture ?: return

        when {
            // Case A: {player} placeholder
            tex.equals("{player}", ignoreCase = true) -> {
                style.offlinePlayer?.let { p ->
                    if (SkullUtils.VersionHelper.HAS_PLAYER_PROFILES) meta.ownerProfile = p.playerProfile
                    else meta.owningPlayer = p
                }
            }
            // Case B: UUID string "[uuid]"
            tex.startsWith("[") && tex.endsWith("]") -> {
                try {
                    val uuidString = tex.substring(1, tex.length - 1)
                    val uuid = UUID.fromString(uuidString)
                    val owner = Bukkit.getOfflinePlayer(uuid)
                    if (!owner.isOnline) {
                        val fileTexture = SkinRestorerHook.getSkin(uuid)
                        if (fileTexture != null) SkullUtils.applySkin(meta, SkullProfileCache.getProfile(fileTexture))
                        else if (SkullUtils.VersionHelper.HAS_PLAYER_PROFILES) meta.ownerProfile = owner.playerProfile else meta.owningPlayer = owner
                        return
                    }
                    if (SkullUtils.VersionHelper.HAS_PLAYER_PROFILES) meta.ownerProfile = owner.playerProfile else meta.owningPlayer = owner
                    SkinRestorerHook.cache(uuid, null)
                    return

                } catch (_: Exception) {/* Ignore malformed UUID */
                }
            }
            // Case C: Base64 Texture
            else -> {
                SkullUtils.applySkin(meta, SkullProfileCache.getProfile(tex))
            }
        }
    }



    private fun isEmpty(): Boolean {
        return name.isEmpty() && currentName.isEmpty() && lore.isEmpty() && currentLore.isEmpty()
    }

    private fun isPlayerHead(): Boolean {
        return material.name.contains("PLAYER_HEAD", ignoreCase = true) || material.name.contains("SKULL_ITEM", ignoreCase = true)
    }
}
