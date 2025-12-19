package net.justlime.limeframegui.models

/**
 * Configuration for the Lazy-Loading system of a GUI session.
 * * This class defines the "Sliding Window" parameters that determine when
 * new pages are rendered and when old pages are cleared from memory.
 */
data class GuiBuffer(
    /**
     * @property renderLimit The size of the "batch" or "chunk" of pages to load.
     * When the user approaches the edge of the current buffer, this many pages
     * will be rendered in a single operation. Larger values reduce the frequency
     * of loads but cause larger single-frame performance spikes.
     */
    var renderLimit: Int = 10,
    /**
     * @property margin The "look-ahead" threshold. When the user is within
     * this distance of the last (or first) rendered page index, the next
     * [renderLimit] batch is triggered.
     * Example: If margin is 3, and you reach page 7 of 10, pages 11-20 will load.
     */
    var margin: Int = 3,
    /**
     * @property cleanupMargin The distance from the current page outside which
     * inventories will be cleared. This acts as a "Memory Safety" buffer.
     * If the user's distance from a page index exceeds this value, the inventory
     * is cleared using `.clear()`.
     * Set to `0` or a negative value to disable automatic cleanup.
     */
    var cleanupMargin: Int = 15
)
