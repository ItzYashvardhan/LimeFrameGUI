package net.justlime.limeframegui.models

import net.justlime.limeframegui.models.registry.GuiSound

data class AnvilGuiSetting(
    val title: String,
    val label: String = "",
    val leftItem: GuiItem,
    val rightItem: GuiItem,
    val outPutItem: GuiItem,

    // Compiled lists for instant playback
    val openSounds: List<GuiSound> = emptyList(),
    val cancelSounds: List<GuiSound> = emptyList(),
    val submitSounds: List<GuiSound> = emptyList(),

    // Raw aliases for safe config saving
    val openSoundAlias: String? = null,
    val cancelSoundAlias: String? = null,
    val submitSoundAlias: String? = null,

    val style: GuiStyleSheet,
    val preventClose: Boolean = false
) {
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

            style = style.clone(),
            preventClose = preventClose
        )
    }
}