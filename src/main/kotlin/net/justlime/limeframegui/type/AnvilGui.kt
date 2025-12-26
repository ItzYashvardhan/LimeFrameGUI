package net.justlime.limeframegui.type

import net.justlime.limeframegui.impl.AnvilEventImpl
import net.justlime.limeframegui.impl.AnvilGUIBuilder
import net.justlime.limeframegui.models.AnvilGuiSetting
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiStyleSheet
import org.bukkit.Material
import org.bukkit.entity.Player

class AnvilGUI(private val setting: AnvilGuiSetting, private val block: AnvilGUIBuilder.() -> Unit = {}) {

    /**
     * Convenience Constructor for creating simple Anvils without a config object.
     */
    constructor(title: String, label: String = "", block: AnvilGUIBuilder.() -> Unit = {}) : this(
        AnvilGuiSetting(
            title = title,
            label = label,
            leftItem = GuiItem(Material.PAPER, "Input"),
            rightItem = GuiItem(Material.AIR, " "),
            outPutItem = GuiItem(Material.BARRIER, "Submit"),
            style = GuiStyleSheet()
        ),
        block
    )

    fun open(player: Player) {
        val builder = AnvilGUIBuilder()
        builder.title = setting.title
        builder.label = setting.label
        builder.style = setting.style.copy()

        builder.leftItem = setting.leftItem.clone()
        builder.rightItem = setting.rightItem.clone()
        builder.outputItem = setting.outPutItem.clone()

        builder.openSound = setting.openSound
        builder.cancelSound = setting.cancelSound
        builder.submitSound = setting.submitSound


        builder.apply(block)
        val runtimeSetting = builder.buildSetting()
        AnvilEventImpl(player, runtimeSetting, builder).open()
    }
}