package net.justlime.limeframegui.models.registry

// The Root Package (Can be a Standard Pack or a Sequence of Packs)
sealed class GuiActionPack {

    data class Standard(
        val requirements: List<String> = emptyList(),
        val denyBehavior: ActionBehavior = ActionBehavior.Simple(emptyList()),
        val clickActions: Map<String, ActionBehavior> = emptyMap()
    ) : GuiActionPack()

    data class Sequence(
        val nodes: List<Standard>,
        val fallback: Standard? = null // The 'else' block
    ) : GuiActionPack()
}

// What actually happens when an action is triggered (A List or a Switch-Case)
