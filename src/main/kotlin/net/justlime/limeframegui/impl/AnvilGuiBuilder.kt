package net.justlime.limeframegui.impl

import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSound
import net.justlime.limeframegui.models.GuiStyleSheet
import org.bukkit.Material
import org.bukkit.entity.Player

class AnvilGUIBuilder {

    // --- Configuration ---
    var title: String = "Anvil GUI"
    var label: String = ""
    var style: GuiStyleSheet = GuiStyleSheet()
    var preventClose: Boolean = false // Default false

    // --- Items ---
    var leftItem: GuiItem = GuiItem(Material.PAPER, "Input")
    var rightItem: GuiItem = GuiItem(Material.AIR, " ")
    var outputItem: GuiItem = GuiItem(Material.BARRIER, "Submit")

    // --- Sounds ---
    var submitSound: GuiSound? = null
    var cancelSound: GuiSound? = null
    var openSound: GuiSound? = null

    // --- Callbacks ---
    internal var onConfirmHandler: ((Player, String) -> Unit)? = null
    internal var onCancelHandler: ((Player) -> Unit)? = null
    internal var onInvalidInputHandler: ((Player) -> Unit)? = null
    internal var onCloseHandler: ((Player) -> Unit)? = null

    // --- DSL Setters ---
    fun onConfirm(block: (Player, String) -> Unit) { onConfirmHandler = block }
    fun onCancel(block: (Player) -> Unit) { onCancelHandler = block }
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
            openSound = openSound,
            cancelSound = cancelSound,
            submitSound = submitSound,
            style = style,
            preventClose = preventClose
        )
    }
}