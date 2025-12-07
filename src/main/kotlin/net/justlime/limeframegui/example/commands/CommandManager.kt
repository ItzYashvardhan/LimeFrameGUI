package net.justlime.limeframegui.example.commands

import net.justlime.limeframegui.handle.CommandHandler
import org.bukkit.plugin.java.JavaPlugin

class CommandManager(val plugin: JavaPlugin) {
    private val simpleGUI = SimpleGUICommand()
    private val commandList = mutableMapOf<String, CommandHandler>()

    init {
        commandList["simplegui"] = simpleGUI
        initializeCommand()
    }

    fun initializeCommand() {
        commandList.forEach { (command, handle) ->
            plugin.getCommand(command)?.apply {
                setExecutor(handle)
                tabCompleter = handle
                permission = handle.permission
                aliases = handle.aliases
            }
        }
    }

}
