package net.justlime.limeframegui.util

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