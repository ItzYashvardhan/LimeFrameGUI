package net.justlime.limeframegui.models

import net.justlime.limeframegui.context.IContextSetting
import net.justlime.limeframegui.models.registry.ActionBehavior
import net.justlime.limeframegui.models.registry.GuiSound

data class AnvilGuiSetting(
    override var title: String,
    var label: String = "",
    var leftItem: GuiItem,
    var rightItem: GuiItem,
    var outPutItem: GuiItem,

    // Compiled lists for instant playback
    val openSounds: List<GuiSound> = emptyList(),
    val cancelSounds: List<GuiSound> = emptyList(),
    val submitSounds: List<GuiSound> = emptyList(),

    // Raw aliases for safe config saving
    val openSoundAlias: String? = null,
    val cancelSoundAlias: String? = null,
    val submitSoundAlias: String? = null,

    override var style: GuiStyleSheet,
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
            cancelSounds = ArrayList(cancelSounds),
            submitSounds = ArrayList(submitSounds),

            // Pass the raw aliases
            openSoundAlias = openSoundAlias,
            cancelSoundAlias = cancelSoundAlias,
            submitSoundAlias = submitSoundAlias,

            style = style.copy(),
            preventClose = preventClose,

            // Fix: Added missing deep copies!
            openRequirements = ArrayList(openRequirements),
            denyBehavior = denyBehavior,
            localVariables = HashMap(localVariables),
            localPlaceholders = HashMap(localPlaceholders)
        )
    }
}