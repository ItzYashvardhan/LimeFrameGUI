package net.justlime.limeframegui.models.parser

/**
 * Encapsulates the layout patterns extracted from a configuration layer.
 * Maps permission nodes to their specific row configurations.
 */
data class PatternVariants(
    val layouts: Map<String, Map<Int, String>>
) {
    /** Returns all explicitly defined permission nodes in this pattern set. */
    val permissions: Set<String> get() = layouts.keys

    /** True if this configuration only contains a "default" pattern with no permissions. */
    val isDefaultOnly: Boolean
        get() = layouts.size == 1 && layouts.containsKey("default")

    /**
     * Safely retrieves the pattern layout for a given permission.
     * Falls back to the "default" layout if the specific permission pattern doesn't exist.
     */
    fun getLayout(permission: String): Map<Int, String>? {
        return layouts[permission] ?: layouts["default"]
    }
}