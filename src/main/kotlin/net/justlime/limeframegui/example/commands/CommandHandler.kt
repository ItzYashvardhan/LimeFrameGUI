package net.justlime.limeframegui.example.commands

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

interface CommandHandler: CommandExecutor, TabCompleter {

    val permission: String
        get() = ""
    val aliases: List<String>
        get() = mutableListOf()

}