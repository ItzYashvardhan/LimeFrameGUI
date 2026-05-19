package net.justlime.limeframegui.manager

import net.justlime.limeframegui.menu.ChestGUI
import net.justlime.limeframegui.models.GuiPageTemplate
import net.justlime.limeframegui.registry.component.LangRegistry
import net.justlime.limeframegui.registry.gui.ListPopulatorRegistry
import net.justlime.limeframegui.registry.gui.PageRegistry
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * The central rendering engine for the GUI framework.
 * Responsible for taking compiled, static blueprints (GuiPageTemplate) from the registry,
 * applying the viewer's context (placeholders, themes, permissions), and assembling
 * the interactive inventory safely.
 */
object GuiManager {

    /**
     * Constructs and opens a GUI page for the specified player.
     *
     * @param player The player opening the GUI.
     * @param guiId The registered ID/namespace of the page to open.
     * @return True if the page was successfully found and opened, false otherwise.
     */
    fun open(player: Player, guiId: String): Boolean {
        val template: GuiPageTemplate = PageRegistry.get(guiId) ?: run {
            println("[LimeFrameGUI] Error: Attempted to open unknown page '$guiId'")
            return false
        }
        val locale = player.locale
        val resolvedTitle = LangRegistry.resolveLangString(template.setting.title, locale)
        val localizedSetting = template.setting.copy(title = resolvedTitle)
        ChestGUI(localizedSetting) {
            onClick { it.isCancelled = true }

            // 1. Resolve Permission-Based Layout
            // We find the first permission the player has, or fallback to "default"
            val activeItems = template.permissionItems.entries
                .firstOrNull { (perm, _) -> perm == "default" || player.hasPermission(perm) }
                ?.value ?: template.permissionItems["default"] ?: emptyList()

            val nextBtn = activeItems.find { it.style.action == "core_next_page" }
            val prevBtn = activeItems.find { it.style.action == "core_prev_page" }

            // 2. Configure Dynamic List Navigation
            template.dynamicMask?.let { mask ->
                nav {
                    nextBtn?.let {
                        nextSlot = it.slot ?: 0
                        nextItem = it.clone().apply { style.viewer = player }
                    }
                    prevBtn?.let {
                        prevSlot = it.slot ?: 0
                        prevItem = it.clone().apply { style.viewer = player }
                    }

                    buffer {
                        renderLimit = mask.buffer.renderLimit
                        margin = mask.buffer.margin
                        cleanupMargin = mask.buffer.cleanupMargin
                    }
                }
            }

            // 3. Render Static Layout & Semantic Items
            activeItems.forEach { templateItem ->
                val action = templateItem.style.action
                if (action == "core_next_page" || action == "core_prev_page") return@forEach
                if (templateItem.material == Material.AIR) return@forEach

                val playerItem = templateItem.clone()
                playerItem.style.viewer = player

                playerItem.name = LangRegistry.resolveLangString(playerItem.name, locale)
                playerItem.lore = playerItem.lore.map { LangRegistry.resolveLangString(it, locale) }

                setItem(playerItem) { event ->
                    playerItem.onClick(event)
                }
            }

            // 4. Populate and Render Dynamic Elements
            template.dynamicMask?.let { mask ->
                // Fetch context-aware items (e.g., list of online players, allies, etc.)
                val populatedItems = ListPopulatorRegistry.getItems(mask.populatorId, player, mask.templateItem)

                // Hand over to ChestGUI's pagination engine
                addPage {
                    populatedItems.forEach { item ->
                        addItem(item) { event ->
                            item.onClick(event)
                        }
                    }
                }
            }

        }.open(player)
        return true
    }
}