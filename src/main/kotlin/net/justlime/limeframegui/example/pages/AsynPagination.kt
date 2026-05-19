package net.justlime.limeframegui.example.pages

import net.justlime.limeframegui.api.LimeFrameAPI
import net.justlime.limeframegui.models.GuiItem
import net.justlime.limeframegui.models.GuiSetting
import net.justlime.limeframegui.menu.ChestGUI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

private object MockCodeDAO {
    /**
     * Simulates fetching data from a database with a 1-second delay.
     * Generates codes like "COD-1", "COD-2", etc.
     */
    fun fetchAsync(limit: Int, offset: Int, callback: (List<String>) -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(LimeFrameAPI.getPlugin(), Runnable {
            println("[MockDB] Fetching $limit items starting at $offset...")

            try {
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // If offset is 0 and limit is 45 -> Returns [COD-1-SAMPLE ... COD-45-SAMPLE]
            val data = (1..limit).map { i ->
                val id = offset + i
                "COD-${id}-SAMPLE"
            }
            Bukkit.getScheduler().runTaskLater(LimeFrameAPI.getPlugin(), Runnable {
                callback(data)
            }, 20)
        })
    }
}

/**
 * Opens the Async Test GUI.
 * @param viewer The player opening the GUI (Required for openDynamic)
 */
fun openAsyncGui(viewer: Player, setting: GuiSetting, pageId: Int) {

    ChestGUI(setting) {
        onClick { it.isCancelled = true }

        nav {
            buffer {
                renderLimit = 4
                margin = 1
                cleanupMargin = 8
            }
        }

        val item = GuiItem(Material.LIME_STAINED_GLASS_PANE, slotList = (45..53).toList())
        setItem(item)

        addPage {
            MockCodeDAO.fetchAsync(450, pageId - 1) { codes ->
                codes.forEach { code ->
                    val item = GuiItem(Material.PAPER, name = code)
                    addItemLater(item) {
                        openPageItem(viewer, viewerPage, setting)
                    }
                }
                val loadedPages = session.builder.pages.size
                println("Loaded: $loadedPages")
//                val lastPage = session.builder.pages[loadedPages-1]
//                lastPage?.onOpen {
//                    session.handler.pageOpenHandlers.remove(loadedPages-1)
//                    refresh()
//                }
            }

        }
    }.open(viewer, pageId)

}

fun openPageItem(viewer: Player, pageId: Int, setting: GuiSetting) {
    ChestGUI(3, "hello") {
        onClick { it.isCancelled = true }
        val item = GuiItem(Material.BARREL, "Go Back")
        setItem(item, 14) {
            openAsyncGui(viewer, setting, pageId)
        }
    }.open(viewer)
}