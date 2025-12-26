package net.justlime.limeframegui.models

import loader.SoundLoader
import net.justlime.limeframegui.integration.FoliaLibHook
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Represents a sound or a collection of sounds to be played.
 * * Format: "SOUND, PITCH, VOLUME, DELAY"
 */
data class GuiSound(
    var sound: String? = null,
    var pitch: Float = 1f,
    var volume: Float = 1f,
    var delay: Long = 0
) : Cloneable {

    private val additionalSounds = mutableListOf<GuiSound>()

    fun addSound(other: GuiSound) {
        additionalSounds.add(other)
    }

    public override fun clone(): GuiSound {
        val copy = this.copy()
        this.additionalSounds.forEach {
            copy.addSound(it.clone())
        }
        return copy
    }

    companion object {
        @Suppress("DEPRECATION")
        private val sounds = Sound.values().map { it.name() }

        fun getSounds(): List<String> = sounds

        fun loadSound(input: Any?, ignoreRegistry: Boolean = false): GuiSound {
            // Registry Lookup
            if (!ignoreRegistry && input is String) {
                val registered = SoundLoader.get(input)
                if (registered != null) return registered
            }

            // Parse Standard String
            return when (input) {
                is String -> parseSingle(input)
                is List<*> -> {
                    val strList = input.filterIsInstance<String>()
                    if (strList.isEmpty()) return GuiSound()
                    val mainSound = parseSingle(strList[0])
                    for (i in 1 until strList.size) {
                        mainSound.addSound(parseSingle(strList[i]))
                    }
                    mainSound
                }
                else -> GuiSound()
            }
        }

        private fun parseSingle(line: String): GuiSound {
            val finalSound = GuiSound()
            val parts = line.split(",").map { it.trim() }
            if (parts.isEmpty() || parts[0].isEmpty()) return finalSound

            finalSound.sound = parts[0]

            var numericSlot = 0
            for (i in 1 until parts.size) {
                val token = parts[i]
                val lowerToken = token.lowercase()
                when {
                    lowerToken.endsWith("p") -> finalSound.pitch = token.dropLast(1).toFloatOrNull() ?: 1.0f
                    lowerToken.endsWith("v") -> finalSound.volume = token.dropLast(1).toFloatOrNull() ?: 1.0f
                    lowerToken.endsWith("d") || lowerToken.endsWith("t") -> finalSound.delay = token.dropLast(1).toLongOrNull() ?: 0L
                    else -> {
                        val num = token.toFloatOrNull()
                        if (num != null) {
                            when (numericSlot) {
                                0 -> finalSound.pitch = num
                                1 -> finalSound.volume = num
                                2 -> finalSound.delay = num.toLong()
                            }
                            numericSlot++
                        }
                    }
                }
            }
            return finalSound
        }
    }

    fun isEmpty(): Boolean = sound == null && additionalSounds.isEmpty()

    /**
     * Starts the sound playback sequence.
     * Uses a Sequential Timeline (Accumulated Delay).
     */
    fun playSound(player: Player): Boolean {
        if (!player.isOnline) return false


        val queue = mutableListOf<GuiSound>()
        if (sound != null) queue.add(this)
        queue.addAll(additionalSounds)

        if (queue.isEmpty()) return false

        var timeTracker = 0L // The timeline cursor (starts at 0 ticks)

        queue.forEach { part ->
            timeTracker += part.delay
            resolveAndSchedule(player, part, timeTracker, 0)
        }
        return true
    }

    /**
     * Recursively resolves aliases and schedules the final NMS sounds.
     * @param absoluteDelay The calculated time (in ticks from NOW) when this sound should play.
     */
    private fun resolveAndSchedule(player: Player, request: GuiSound, absoluteDelay: Long, depth: Int) {
        if (depth > 5 || request.sound == null) return

        val alias = SoundLoader.get(request.sound!!)

        if (alias != null && alias !== this && alias !== request) {


            val aliasParts = mutableListOf<GuiSound>()
            if (alias.sound != null) aliasParts.add(alias)
            aliasParts.addAll(alias.additionalSounds)


            var internalTracker = absoluteDelay

            aliasParts.forEach { part ->
                internalTracker += part.delay

                // Clone to apply overrides
                val resolvedPart = part.clone()

                if (request.pitch != 1.0f) resolvedPart.pitch = request.pitch
                if (request.volume != 1.0f) resolvedPart.volume = request.volume

                // Recurse!
                resolveAndSchedule(player, resolvedPart, internalTracker, depth + 1)
            }
            return
        }

        // REAL SOUND

        if (absoluteDelay <= 0) {
            playBukkitSound(player, request)
        } else {
            scheduleBukkitSound(player, request, absoluteDelay)
        }
    }

    private fun scheduleBukkitSound(player: Player, audio: GuiSound, delayTicks: Long) {
        if (FoliaLibHook.isInitialized()) {
            FoliaLibHook.foliaLib.scheduler.runLater(Runnable {
                playBukkitSound(player, audio)
            }, delayTicks)
            return
        }
        try {
            val plugin = JavaPlugin.getProvidingPlugin(GuiSound::class.java)
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                playBukkitSound(player, audio)
            }, delayTicks)
        } catch (_: Exception) {
            playBukkitSound(player, audio)
        }
    }

    private fun playBukkitSound(player: Player, audio: GuiSound) {
        if (!player.isOnline) return
        try {
            player.playSound(player.location, audio.sound!!.lowercase(), audio.volume, audio.pitch)
        } catch (_: Exception) { }
    }

    fun stopSound(player: Player): Boolean {
        if (sound == null) return false
        player.stopSound(sound!!)
        additionalSounds.forEach {
            if (it.sound != null) player.stopSound(it.sound!!)
        }
        return true
    }
}