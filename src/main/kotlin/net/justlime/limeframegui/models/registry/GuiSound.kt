package net.justlime.limeframegui.models.registry

import net.justlime.limeframegui.integration.FoliaLibHook
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Represents a single, pre-calculated sound instruction.
 * Complex packs are represented as a List<GuiSound>, handled by the SoundRegistry.
 */
data class GuiSound(
    var sound: String? = null,
    var pitch: Float = 1f,
    var volume: Float = 1f,
    var delay: Long = 0 // This is now the absolute delay in ticks from the moment of playback
) : Cloneable {

    public override fun clone(): GuiSound = this.copy()

    fun isEmpty(): Boolean = sound == null

    // --- PLAYBACK LOGIC ---

    fun play(player: Player) {
        if (!player.isOnline || sound == null) return

        if (delay <= 0) {
            playBukkitSound(player)
        } else {
            scheduleBukkitSound(player)
        }
    }

    private fun scheduleBukkitSound(player: Player) {
        if (FoliaLibHook.isInitialized()) {
            FoliaLibHook.foliaLib.scheduler.runLater(Runnable {
                playBukkitSound(player)
            }, delay)
            return
        }
        try {
            val plugin = JavaPlugin.getProvidingPlugin(GuiSound::class.java)
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                playBukkitSound(player)
            }, delay)
        } catch (_: Exception) {
            playBukkitSound(player)
        }
    }

    private fun playBukkitSound(player: Player) {
        if (!player.isOnline) return
        try {
            // The registry already verified it's a valid Bukkit enum string
            player.playSound(player.location, sound!!.lowercase(), volume, pitch)
        } catch (_: Exception) { }
    }

    // --- COMPANION HELPERS ---

    companion object {
        /**
         * Plays a pre-compiled list of sounds fetched from the SoundRegistry.
         */
        fun playPack(player: Player, sounds: List<GuiSound>) {
            if (!player.isOnline || sounds.isEmpty()) return

            // Because SoundRegistry calculates absolute delays, we don't need a timeTracker!
            // We just tell every sound to schedule itself.
            for (audio in sounds) {
                audio.play(player)
            }
        }

        /**
         * Stops a list of sounds for a player.
         */
        fun stopPack(player: Player, sounds: List<GuiSound>) {
            if (!player.isOnline) return
            for (audio in sounds) {
                audio.sound?.let { player.stopSound(it) }
            }
        }
    }
}