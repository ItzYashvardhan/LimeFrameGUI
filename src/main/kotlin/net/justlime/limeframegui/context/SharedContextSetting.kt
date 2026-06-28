package net.justlime.limeframegui.context

import net.justlime.limeframegui.models.GuiStyleSheet
import net.justlime.limeframegui.models.registry.ActionBehavior

/**
 * A lightweight, mutable implementation of IContextSetting used exclusively
 * to collect shared header data during the inheritance compilation phase.
 */
data class SharedContextSetting(
    override var title: String = "",
    var label: String = "",
    override var style: GuiStyleSheet = GuiStyleSheet(),
    override var openRequirements: List<String> = emptyList(),
    override var denyBehavior: ActionBehavior = ActionBehavior.Simple(emptyList()),
    override var localVariables: Map<String, String> = emptyMap(),
    override var localPlaceholders: Map<String, String> = emptyMap()
) : IContextSetting