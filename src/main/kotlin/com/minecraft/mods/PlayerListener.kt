package com.minecraft.mods

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerListener: Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        // Only check when player actually moves to a different block
        if (event.from.blockX == event.to.blockX && 
            event.from.blockY == event.to.blockY && 
            event.from.blockZ == event.to.blockZ) {
            return
        }

        if (GameManager.state != GameManager.GameState.RUNNING) return

        val player = event.player
        val data = GameManager.players[player.uniqueId] ?: return
        if(data.completed) return

        val blockBelow = event.to.block.getRelative(0, -1, 0).type

        if(blockBelow == data.targetBlock) {
            data.completed = true
            player.sendMessage("§a✔ You are standing on the given block!")

            if(GameManager.players.values.all { it.completed }) {
                GameManager.endRound()
            }
        }
    }
}