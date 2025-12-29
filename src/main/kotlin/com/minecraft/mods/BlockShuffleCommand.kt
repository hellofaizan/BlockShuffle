package com.minecraft.mods

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class BlockShuffleCommand: CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (args.isEmpty() || args[0] != "start") {
            sender.sendMessage("§cUsage: /bs start")
            return true
        }

        if (GameManager.state == GameManager.GameState.RUNNING) {
            sender.sendMessage("§cGame already started!")
            return true
        }

        GameManager.startGame()
        return true
    }
}