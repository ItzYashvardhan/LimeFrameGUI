package net.justlime.limeframegui.models

import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.GuiSound

data class AnvilGuiSetting(
    override var title: String = "",
    var label: String = "",
    var leftItem: GuiItem = GuiItem(),
    var rightItem: GuiItem = GuiItem(),
    var outPutItem: GuiItem = GuiItem(),

    // Compiled lists for instant playback
    val openSounds: List<GuiSound> = emptyList(),

    // Raw aliases for safe config saving
    var openSoundString: String? = null,

    override var style: GuiStyleSheet = GuiStyleSheet(),
    var preventClose: Boolean = false,
    override var openRequirements: List<String> = emptyList(),
    override var denyBehavior: ActionBehavior = ActionBehavior.Simple(emptyList()),
    override var localVariables: Map<String, String> = emptyMap(),
    override var localPlaceholders: Map<String, String> = emptyMap()
) : IContextSetting {

    fun clone(): AnvilGuiSetting {
        return AnvilGuiSetting(
            title = title,
            label = label,
            leftItem = leftItem.clone(),
            rightItem = rightItem.clone(),
            outPutItem = outPutItem.clone(),

            // Deep copy the compiled lists
            openSounds = ArrayList(openSounds),

            // Pass the raw aliases
            openSoundString = openSoundString,

            style = style.copy(),
            preventClose = preventClose,

            openRequirements = ArrayList(openRequirements),
            denyBehavior = denyBehavior,
            localVariables = HashMap(localVariables),
            localPlaceholders = HashMap(localPlaceholders)
        )
    }
}