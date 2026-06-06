package net.justlime.limeframegui.models

import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * Represents an item in a LimeFrame GUI.
 *
 * @param name The display name of the item.
 * @param lore A list of lore lines displayed under the item name.
 * @param texture Base64-encoded texture string for custom player heads (only works with PLAYER_HEAD).
 * @param slot Single slot index to place this item into.
 * @param slotList Multiple slot indices to place the same item in several slots.
 * @param style StyleSheet to display stylish and dynamic text (i.e, custom placeholder, small cap's font)
 * @param onClick Event callback invoked when this item is clicked in the GUI.
 */
data class GuiItem(
    var baseItem: ItemStack = ItemStack(Material.AIR),
    var baseItemString: String = "",

    // Raw Text Templates
    var name: String = "",
    var lore: List<String> = mutableListOf(),
    val nameState: (() -> String)? = null,
    val loreState: (() -> List<String>)? = null,

    // Conditional View Logic
    var viewRequirements: List<String> = emptyList(),
    var priority: Int = 0,

    // Placement & Ticking
    var slot: Int? = null,
    var slotList: List<Int> = mutableListOf(),
    var updateInterval: Int? = null,

    // Textures & Styles
    var texture: String? = null,
    var style: GuiStyleSheet = GuiStyleSheet(),

    // Data-Driven States
    var stateId: String? = null,
    var states: Map<String, GuiItem> = emptyMap(),

    //Click Handling
    var onClick: (InventoryClickEvent) -> Unit = {}
) {
    constructor(material: Material, name: String = "", lore: List<String> = mutableListOf()) : this(
        baseItem = ItemStack(material),
        name = name,
        lore = lore
    )

    // Helper properties to resolve dynamic state vs static state
    val currentName: String get() = nameState?.invoke() ?: name
    val currentLore: List<String> get() = loreState?.invoke() ?: lore

    /**
     * Creates a deep copy of the GuiItem.
     */
    fun clone(): GuiItem {
        return this.copy(
            baseItem = this.baseItem.clone(),
            lore = ArrayList(this.lore),
            viewRequirements = ArrayList(this.viewRequirements),
            slotList = ArrayList(this.slotList),
            states = this.states.mapValues { it.value.clone() },
            style = this.style.copy(
                placeholder = HashMap(this.style.placeholder),
                textSettings = this.style.textSettings.clone()
            )
        )
    }

}
