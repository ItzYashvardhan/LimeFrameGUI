package net.justlime.limeframegui.models

import net.justlime.limeframegui.models.registry.DynamicListMask

data class GuiPageTemplate(
    val id: String, // e.g., "dashboard"
    val setting: GuiSetting,
    val permissionItems: Map<String, List<GuiItem>> = emptyMap(),
    val dynamicMask: DynamicListMask? = null
)