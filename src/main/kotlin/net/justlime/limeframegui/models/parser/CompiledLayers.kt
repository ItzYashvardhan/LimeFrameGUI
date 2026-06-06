package net.justlime.limeframegui.models.parser

import net.justlime.limeframegui.models.GuiItem

/**
 * Represents the finalized, flattened layout of a GUI page after all inheritance 
 * and permission layers have been compiled.
 *
 * @property permissionItems A map linking a permission node to its fully calculated list of GuiItems.
 * @property dynamicSlots A list of inventory slot indices reserved for the dynamic list populator.
 */
data class CompiledLayers(
    val permissionItems: Map<String, List<GuiItem>>,
    val dynamicSlots: List<Int>
)