package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlEngine
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object ConditionEngine {

    private val jexl: JexlEngine = JexlBuilder()
        .cache(512)
        .strict(true)
        .silent(false)
        .create()


    /**
     * Evaluates a list of requirement strings for a player.
     * Returns TRUE if all requirements are met.
     */
    fun checkRequirements(player: Player, requirements: List<String>): Boolean {
        if (requirements.isEmpty()) return true
        return requirements.all { evaluateSingle(player, it) }
    }

    private fun evaluateSingle(player: Player, conditionString: String): Boolean {
        val str = conditionString.trim()

        // Simple Permission Check: "[permission] betterteams.admin"
        if (str.startsWith("[permission]", ignoreCase = true)) {
            val perm = str.removePrefix("[permission]").trim()
            return player.hasPermission(perm)
        }

        // Simple Group Check (Assuming Vault or LuckPerms API here, simplified for example)
        if (str.startsWith("[group]", ignoreCase = true)) {
            val group = str.removePrefix("[group]").trim()
            return player.hasPermission("group.$group") // Basic LuckPerms fallback check
        }

        // Complex Condition Check
        if (str.startsWith("[condition]", ignoreCase = true)) {
            var mathExpression = str.removePrefix("[condition]").trim()
            mathExpression = mathExpression.replace("{", "%").replace("}", "%")
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                mathExpression = PlaceholderAPI.setPlaceholders(player, mathExpression)
            }
            mathExpression = resolveNestedTags(player, mathExpression)
            return evaluateBooleanLogic(mathExpression)
        }
        return false
    }

    /**
     * Finds tags like [permission] inside a larger condition string and converts them to true/false.
     */
    private fun resolveNestedTags(player: Player, expression: String): String {
        var resolved = expression
        val permRegex = Regex("\\[permission\\]\\s+([a-zA-Z0-9._-]+)")
        val groupRegex = Regex("\\[group\\]\\s+([a-zA-Z0-9._-]+)")

        resolved = permRegex.replace(resolved) { match ->
            player.hasPermission(match.groupValues[1]).toString()
        }
        
        resolved = groupRegex.replace(resolved) { match ->
            player.hasPermission("group.${match.groupValues[1]}").toString()
        }
        
        return resolved
    }

    /**
     * A basic evaluator for strings like "500 >= 100 && true"
     * NOTE: For full robust parsing of () parentheses, consider adding 'exp4j' 
     * or a similar lightweight expression evaluator to your build.gradle!
     */
    private fun evaluateBooleanLogic(expression: String): Boolean {
        return try {
            val cleanExpr = expression
                .replace(" AND ", " && ", ignoreCase = true)
                .replace(" OR ", " || ", ignoreCase = true)
            val jexlExpression = jexl.createExpression(cleanExpr)
            val result = jexlExpression.evaluate(null)
            result as? Boolean ?: false
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[LimeFrameGUI] Failed to evaluate condition: '$expression'. Error: ${e.message}")
            false
        }
    }
}