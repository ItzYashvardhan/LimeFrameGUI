package net.justlime.limeframegui.builder

import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Player

class AnvilGuiBuilder {

    // --- Configuration ---
    var title: String = "Anvil GUI"
    var label: String = ""
    var style: GuiStyleSheet = GuiStyleSheet()
    var preventClose: Boolean = false
    var keepLabel: Boolean = false

    // --- Items ---
    var leftItem: GuiItem = GuiItem(Material.PAPER, "Input")
    var rightItem: GuiItem = GuiItem(Material.AIR, " ")
    var outputItem: GuiItem = GuiItem(Material.BARRIER, "Submit")

    // --- Sounds (Updated to use String Aliases) ---
    var submitSoundAlias: String? = null
    var cancelSoundAlias: String? = null
    var openSoundAlias: String? = null

    // --- Callbacks ---
    internal var onRightClickHandler: ((state: AnvilGUI.StateSnapshot) -> Unit)? = null
    internal var onLeftClickHandler: ((state: AnvilGUI.StateSnapshot) -> Unit)? = null
    internal var onOutputClickHandler: ((state: AnvilGUI.StateSnapshot, input: String) -> Unit)? = null
    internal var onInvalidInputHandler: ((Player) -> Unit)? = null
    internal var onCloseHandler: ((Player) -> Unit)? = null

    fun onRightClick(block: (state: AnvilGUI.StateSnapshot) -> Unit) { onRightClickHandler = block }
    fun onLeftClick(block: (state: AnvilGUI.StateSnapshot) -> Unit) { onLeftClickHandler = block }
    fun onConfirmClick(block: (state: AnvilGUI.StateSnapshot, input: String) -> Unit) { onOutputClickHandler = block }

    fun onInvalidInput(block: (Player) -> Unit) { onInvalidInputHandler = block }
    fun onClose(block: (Player) -> Unit) { onCloseHandler = block }

    /**
     * Prevents the player from closing the GUI with ESC.
     */
    fun preventClose() { this.preventClose = true }

    fun buildSetting(): AnvilGuiSetting {
        return AnvilGuiSetting(
            title = title,
            label = label,
            leftItem = leftItem,
            rightItem = rightItem,
            outPutItem = outputItem,

            // Map the builder aliases to the setting's alias fields
            openSoundAlias = openSoundAlias,
            cancelSoundAlias = cancelSoundAlias,
            submitSoundAlias = submitSoundAlias,

            style = style,
            preventClose = preventClose
        )
    }
}