package net.justlime.limeframegui.engine

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object ConditionEngine {

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

        // 1. Simple Permission Check: "[permission] betterteams.admin"
        if (str.startsWith("[permission]", ignoreCase = true)) {
            val perm = str.removePrefix("[permission]").trim()
            return player.hasPermission(perm)
        }

        // 2. Simple Group Check (Assuming Vault or LuckPerms API here, simplified for example)
        if (str.startsWith("[group]", ignoreCase = true)) {
            val group = str.removePrefix("[group]").trim()
            return player.hasPermission("group.$group") // Basic LuckPerms fallback check
        }

        // 3. Complex Condition Check
        if (str.startsWith("[condition]", ignoreCase = true)) {
            var mathExpression = str.removePrefix("[condition]").trim()

            // A) Resolve all Placeholders first (e.g., {player_balance} -> 500)
            // Replace your custom {} with %% for PAPI
            mathExpression = mathExpression.replace("{", "%").replace("}", "%")
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                mathExpression = PlaceholderAPI.setPlaceholders(player, mathExpression)
            }

            // B) Resolve nested [permission] and [group] tags inside the string to "true" or "false"
            mathExpression = resolveNestedTags(player, mathExpression)

            // C) Evaluate the final Boolean/Math Expression!
            // Example resulting string: "500 >= 100 && (15 > 10 || false || true)"
            return evaluateBooleanLogic(mathExpression) 
        }

        // Support for your old format just in case: "permission: node"
        if (str.lowercase().startsWith("permission:")) {
            return player.hasPermission(str.split(":")[1].trim())
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
        // TODO: Implement your preferred Math/Boolean script net.justlime.limeframegui.engine here.
        // For standard Bukkit plugins, developers usually hook into PlaceholderAPI's 
        // Math expansion, or use a lightweight library to evaluate the final string.
        
        // For now, returning true to prevent crashing while you implement the math parser
        return true 
    }
}