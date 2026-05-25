package net.justlime.limeframegui.context

import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.registry.ActionBehavior

interface IContextSetting {
    val title: String
    val style: GuiStyleSheet
    val openRequirements: List<String>
    val denyBehavior: ActionBehavior
    val localVariables: Map<String, String>
    val localPlaceholders: Map<String, String>
}