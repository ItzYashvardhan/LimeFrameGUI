package net.justlime.limeframegui.models

import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.models.registry.ActionBehavior

data class GuiSetting(
    var rows: Int,
    override var title: String,
    override var style: GuiStyleSheet = GuiStyleSheet(),
    var label: String = "",
    override var openRequirements: List<String> = emptyList(),
    override var denyBehavior: ActionBehavior = ActionBehavior.Simple(emptyList()),
    override var localVariables: Map<String, String> = emptyMap(),
    override var localPlaceholders: Map<String, String> = emptyMap()
) : IContextSetting {

    fun clone(): GuiSetting {
        return GuiSetting(
            rows = rows,
            title = title,
            style = style.copy(),
            label = label,
            openRequirements = ArrayList(openRequirements),
            denyBehavior = denyBehavior,
            localVariables = HashMap(localVariables),
            localPlaceholders = HashMap(localPlaceholders)
        )
    }
}