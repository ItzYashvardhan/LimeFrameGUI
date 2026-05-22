package net.justlime.limeframegui.registry

import net.justlime.limeframegui.engine.ActionTagHandler

object ActionTagRegistry {
    private val handlers = mutableMapOf<String, ActionTagHandler>()
    private var sortedTags = emptyList<Pair<String, ActionTagHandler>>()

    /**
     * Registers a new action tag.
     * @param tag The tag string including brackets (e.g., "[message]", "[open_gui]")
     */
    fun register(tag: String, handler: ActionTagHandler) {
        handlers[tag.lowercase()] = handler

        // Resort whenever a new tag is added
        sortedTags = handlers.entries
            .sortedByDescending { it.key.length }
            .map { it.key to it.value }
    }

    /**
     * Gets the handlers safely sorted by length descending.
     */
    internal fun getTags(): List<Pair<String, ActionTagHandler>> {
        return sortedTags
    }
}