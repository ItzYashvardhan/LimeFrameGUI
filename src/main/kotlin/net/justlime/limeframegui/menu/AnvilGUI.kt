package net.justlime.limeframegui.menu

import net.justlime.limeframegui.builder.AnvilGuiBuilder
import net.justlime.limeframegui.event.AnvilEventImpl
import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * The Blueprint class for Anvil GUIs.
 * This class simply holds the configuration and builder block.
 * When open() is called, it spins up a new isolated AnvilGuiEventImpl session.
 */
class AnvilGUI(val setting: AnvilGuiSetting, val block: AnvilGuiBuilder.() -> Unit = {}) {

    /**
     * Convenience Constructor for creating simple code-based Anvils without a YAML config.
     */
    constructor(title: String, label: String = "", block: AnvilGuiBuilder.() -> Unit = {}) : this(
        AnvilGuiSetting(
            title = title,
            label = label,
            leftItem = GuiItem(baseItem = ItemStack(Material.PAPER), name = "Input"),
            rightItem = GuiItem(baseItem = ItemStack(Material.AIR), name = " "),
            outPutItem = GuiItem(baseItem = ItemStack(Material.BARRIER), name = "Submit"),
            style = GuiStyleSheet()
        ),
        block
    )

    /**
     * Opens the Anvil for a specific player.
     * Creates a deep-cloned runtime setting so data doesn't leak between players.
     */
    fun open(player: Player) {
        // 1. Deep clone the blueprint so concurrent players don't overwrite each other's data
        val runtimeSetting = setting.clone()

        // 2. Prepare the Style Context for this specific player
        runtimeSetting.style.viewer = player
        if (runtimeSetting.style.offlinePlayer == null) {
            runtimeSetting.style.offlinePlayer = player
        }

        // 3. Initialize the builder with the cloned runtime setting
        val builder = AnvilGuiBuilder(runtimeSetting)

        // 4. Apply any DSL block modifications (e.g., adding dynamic onClick listeners)
        builder.apply(block)

        // 5. Spin up the Event Implementation (The "Session" for Anvils)
        AnvilEventImpl(player, runtimeSetting, builder).open()
    }
}