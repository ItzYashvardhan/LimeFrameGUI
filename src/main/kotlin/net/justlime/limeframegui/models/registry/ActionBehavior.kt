package net.justlime.limeframegui.models.registry

sealed class ActionBehavior {

    // Just a normal list of tags: - "[message] hello"
    data class Simple(val actions: List<String>) : ActionBehavior()

    // A Switch-Case block!
    data class When(
        val valuePlaceholder: String,
        val results: Map<String, ActionBehavior> // Recursive! A result could contain another When block!
    ) : ActionBehavior()
}