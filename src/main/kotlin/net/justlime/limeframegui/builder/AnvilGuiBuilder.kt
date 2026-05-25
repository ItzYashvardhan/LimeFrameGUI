package net.justlime.limeframegui.builder

import net.justlime.limeframegui.models.AnvilGuiSetting
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.entity.Player

class AnvilGuiBuilder(val setting: AnvilGuiSetting) {

    // ========================================================================
    // --- DSL Convenience Delegates ---
    // These map directly to the underlying 'setting' object so your existing
    // code-based GUIs don't break, while preventing memory duplication!
    // ========================================================================

    var title: String
        get() = setting.title
        set(value) { setting.title = value }

    var label: String
        get() = setting.label
        set(value) { setting.label = value }

    var preventClose: Boolean
        get() = setting.preventClose
        set(value) { setting.preventClose = value }

    var leftItem
        get() = setting.leftItem
        set(value) { setting.leftItem = value }

    var rightItem
        get() = setting.rightItem
        set(value) { setting.rightItem = value }

    var outputItem
        get() = setting.outPutItem // Maps to outPutItem in the data class
        set(value) { setting.outPutItem = value }

    // ========================================================================
    // --- Runtime Options & Callbacks ---
    // These are transient states that don't belong in the YAML blueprint.
    // ========================================================================

    var keepLabel: Boolean = false

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
}