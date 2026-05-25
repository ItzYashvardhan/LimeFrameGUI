package net.justlime.limeframegui.registry.input

import net.justlime.limeframegui.models.AnvilGuiSetting

object InputRegistry {
    private val inputs = mutableMapOf<String, AnvilGuiSetting>()

    fun register(id: String, setting: AnvilGuiSetting) {
        inputs[id] = setting
    }

    fun getInputs(): Map<String, AnvilGuiSetting> = inputs

    fun get(id: String): AnvilGuiSetting? = inputs[id]

    fun clear() {
        inputs.clear()
    }
}