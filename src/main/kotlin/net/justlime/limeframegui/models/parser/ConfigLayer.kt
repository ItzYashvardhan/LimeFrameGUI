package net.justlime.limeframegui.models.parser

import org.bukkit.configuration.file.YamlConfiguration

/**
 * Represents a single compiled layer within a GUI configuration's inheritance chain.
 *
 * When the TemplateCompiler resolves templates (e.g., `inherit: [ "template/background --B" ]`),
 * it generates a ConfigLayer for each step in the chain. This data class binds the raw YAML
 * configuration to its specific cherry-picking rules, ensuring that character exclusions
 * are strictly applied to the correct template layer.
 *
 * @property config The parsed YAML configuration for this specific layer (a parent template or the base child page).
 * @property excludedChars A list of characters that should be ignored when mapping this layer's items to the pattern.
 * For example, if a template is inherited with `--B`, 'B' is added to this list,
 * preventing the compiler from placing the 'B' item from this specific layer.
 */
data class ConfigLayer(
    val config: YamlConfiguration,
    val excludedChars: List<Char>
)