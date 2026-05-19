package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection

interface IRegistry {
    /**
     * Clears all existing data and loads the new section.
     * Use this for LimeFrameGUI's internal reloads.
     */
    fun load(section: ConfigurationSection)

    /**
     * Appends new data to the registry without clearing existing data.
     * Use this for external developers injecting their own files.
     */
    fun append(section: ConfigurationSection)

    fun clear()
}