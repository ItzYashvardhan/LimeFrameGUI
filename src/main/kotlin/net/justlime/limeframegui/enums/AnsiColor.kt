package net.justlime.limeframegui.enums

enum class AnsiColor(val code: String) {
    RED("\u001B[31m"), GREEN("\u001B[32m"), YELLOW("\u001B[33m"), BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"), CYAN("\u001B[36m"), WHITE("\u001B[37m"), ORANGE("\u001B[38;5;208m"),
    GRAY("\u001B[90m"), RESET("\u001B[0m"), BOLD("\u001B[1m"), ITALIC("\u001B[3m"),
    BRIGHT_RED("\u001B[91m"), BRIGHT_GREEN("\u001B[92m"), BRIGHT_YELLOW("\u001B[93m"),
    BRIGHT_BLUE("\u001B[94m"), BRIGHT_PURPLE("\u001B[95m"), BRIGHT_CYAN("\u001B[96m");

    override fun toString(): String = code
}