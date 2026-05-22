package net.justlime.limeframegui.models

import net.justlime.limeframegui.models.registry.ActionBehavior

data class GuiSetting(
    var rows: Int,
    var title: String,
    var style: GuiStyleSheet = GuiStyleSheet(),
    var label: String = "",
    var openRequirements: List<String> = emptyList(),
    var denyBehavior: ActionBehavior = ActionBehavior.Simple(emptyList())
) {
    fun clone(): GuiSetting {
        return GuiSetting(rows, title, style.copy(), label,openRequirements.toList(),denyBehavior)
    }
}