package net.justlime.limeframegui.handler

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

interface CommandHandler: CommandExecutor, TabCompleter {

    val permission: String
        get() = ""
    val aliases: List<String>
        get() = mutableListOf()

}