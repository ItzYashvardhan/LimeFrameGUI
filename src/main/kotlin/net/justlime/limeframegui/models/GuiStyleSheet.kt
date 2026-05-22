package net.justlime.limeframegui.models

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.models.registry.GuiSound
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

data class GuiStyleSheet(
    var viewer: Player? = null,
    var offlinePlayer: OfflinePlayer? = null,
    var placeholder: MutableMap<String, String> = mutableMapOf(),

    // The Advanced Text Formatting Block
    var textSettings: GuiTextSettings = GuiTextSettings(),

    // The compiled lists for instant playback
    var clickSounds: List<GuiSound> = emptyList(),
    var openSounds: List<GuiSound> = emptyList(),
    var closeSounds: List<GuiSound> = emptyList(),

    // The raw aliases for safe saving
    var clickSoundAlias: String? = null,
    var openSoundAlias: String? = null,
    var closeSoundAlias: String? = null,
    var action: String? = null
) {

    fun isEmpty(): Boolean {
        return viewer == null &&
                offlinePlayer == null &&
                placeholder.isEmpty() &&
                clickSoundAlias == null &&
                openSoundAlias == null &&
                closeSoundAlias == null &&
                action == null &&
                clickSounds.isEmpty() &&
                openSounds.isEmpty() &&
                closeSounds.isEmpty() &&
                textSettings.isEmpty()

    }

    fun clone(): GuiStyleSheet {
        return GuiStyleSheet(
            viewer = viewer,
            offlinePlayer = offlinePlayer,
            placeholder = placeholder.toMutableMap(),

            // Deep copy the lists
            clickSounds = ArrayList(clickSounds),
            openSounds = ArrayList(openSounds),
            closeSounds = ArrayList(closeSounds),

            // aliases
            clickSoundAlias = clickSoundAlias,
            openSoundAlias = openSoundAlias,
            closeSoundAlias = closeSoundAlias,

            action = action,
            textSettings = textSettings.clone()

        )
    }
}