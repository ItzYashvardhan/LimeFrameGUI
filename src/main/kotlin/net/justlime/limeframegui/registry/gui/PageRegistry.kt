package net.justlime.limeframegui.registry.gui

import net.justlime.limeframegui.models.GuiPageTemplate

object PageRegistry {
    private val pages = mutableMapOf<String, GuiPageTemplate>()

    fun register(template: GuiPageTemplate) {
        pages[template.id] = template
    }

    fun getPages(): Map<String, GuiPageTemplate> {
        return pages
    }

    fun get(id: String): GuiPageTemplate? {
        return pages[id]
    }

    fun clear() {
        pages.clear()
    }

}