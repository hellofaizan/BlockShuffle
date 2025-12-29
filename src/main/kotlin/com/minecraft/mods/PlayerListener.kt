package com.minecraft.mods

import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class PlayerListener: Listener {

    fun onMove(event: PlayerMoveEvent) {
        if (GameManager.state != GameManager.GameState.RUNNING) return

        val player = event.player
        val data = GameManager.players[player.uniqueId] ?: return
        if(data.completed) return

        val blockBelow = player.location.subtract(0.0, 1.0, 0.0).block.type

        if(blockBelow == data.targetBlock) {
            data.completed = true
            player.sendMessage("§a✔ You are standing on the given block!")

            if(GameManager.players.values.all { it.completed }) {
                GameManager.endRound()
            }
        }
    }
}