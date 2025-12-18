/*
 * RedeemCodeX - Plugin License Agreement
 * Copyright © 2024 Yashvardhan
 *
 * This software is a paid plugin developed by Yashvardhan ("Author") and is provided to you ("User") under the following terms:
 *
 * 1. Usage Rights:
 *    - This plugin is licensed, not sold.
 *    - One license grants usage on **one server network only**, unless explicitly agreed otherwise.
 *    - You may not sublicense, share, leak, or resell the plugin or any part of it.
 *
 * 2. Restrictions:
 *    - You may not decompile, reverse engineer, or modify the plugin.
 *    - You may not redistribute the plugin in any form.
 *    - You may not upload this plugin to any public or private repository or distribution platform.
 *
 * 3. Support & Updates:
 *    - Support is provided to verified buyers only.
 *    - Updates are available as long as development continues or within the support duration stated at purchase.
 *
 * 4. Termination:
 *    - Any violation of this agreement terminates your rights to use this plugin immediately, without refund.
 *
 * 5. No Warranty:
 *    - The plugin is provided "as is", without warranty of any kind. Use at your own risk.
 *    - The Author is not responsible for any damages, data loss, or server issues resulting from usage.
 *
 * For inquiries,
 * Email: itsyashvardhan76@gmail.com
 * Discord: https://discord.gg/rVsUJ4keZN
 */

package net.justlime.limeframegui.utilities

import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

/**
 * A utility to benchmark code execution time and memory allocation.
 * Usage:
 * 1. PerformanceMonitor.start("MyTask") -> ... -> PerformanceMonitor.stop("MyTask")
 * 2. PerformanceMonitor.measure("MyTask") { ... code ... }
 */
object PerformanceMonitor {

    var enable = false

    fun enable() {
        enable = true
    }


    private val tasks = ConcurrentHashMap<String, TaskData>()
    private val runtime = Runtime.getRuntime()

    data class TaskData(val startTime: Long, val startMemory: Long)

    /**
     * Starts tracking a task.
     */
    fun start(name: String) {
        if (!enable) return
        // System.gc()

        val mem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
        tasks[name] = TaskData(System.nanoTime(), mem)
    }

    /**
     * Stops the task and prints the results to Console.
     * @return The formatted log string (optional)
     */
    fun stop(name: String): String {
        if (!enable) return ""
        val data = tasks.remove(name) ?: return "Task '$name' not found."

        val endTime = System.nanoTime()
        val endMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB

        val durationNs = endTime - data.startTime
        val durationMs = durationNs / 1_000_000.0
        val memDiff = endMem - data.startMemory

        // 1 Tick = 50ms
        val ticks = durationMs / 50.0

        // Color coding based on lag severity
        val color = if (durationMs > 50) "§c" else if (durationMs > 10) "§e" else "§a"

        val msg = "[Benchmark] $name: $color${String.format("%.2f", durationMs)}ms §7(${String.format("%.2f", ticks)} ticks) | RAM: ${if (memDiff >= 0) "+" else ""}$memDiff MB"

        Bukkit.getConsoleSender().sendMessage(msg)
        return msg
    }

    /**
     * The Kotlin Way: Benchmarks a block of code automatically.
     */
    inline fun <T> measure(name: String, block: () -> T): T {
        if (!enable) return block()
        start(name)
        try {
            return block()
        } finally {
            stop(name)
        }
    }
}