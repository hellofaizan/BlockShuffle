package com.minecraft.mods

import org.bukkit.Bukkit
import org.bukkit.Material
import java.util.*

object GameManager {

    enum class GameState {
        WAITING,
        RUNNING,
        ENDED
    }

    data class PlayerData(
        val uuid: UUID,
        val targetBlock: Material,
        val completed: Boolean = false
    )

    val state = GameState.WAITING
    var round = 1
    var players = mutableMapOf<UUID, PlayerData>()

    fun startGame() {
        state = GameState.RUNNING
        round = 1
        players.clear()

        Bukkit.broadcastMessage("§a\uD83D\uDFE2 Block Shuffle Started!")

        for (player in Bukkit.getOnlinePlayers()) {
            players[player.uniqueId] = PlayerData(
                player.uniqueId,
                randomBlock()
            )
            player.sendMessage("§eYour block: §6\${players[player.uniqueId]!!.targetBlock}")
        }
        startTimer()
    }

    fun startTimer() {
        var timeLeft = 300
        Bukkit.getScheduler().runTaskTimer(BlockShuffle.instance, Runnable {
            if (state != GameState.RUNNING) return@Runnable

            if (timeLeft <= 0) {
                endRound()
                return@Runnable
            }
            timeLeft--
        }, 0L, 20L)
    }

    fun endRound() {
        val failed = players.values.filter { !it.completed }

        failed.forEach {
            val p = Bukkit.getPlayer(it.uuid)
            p?.sendMessage("§cYou failed this round")
            players.remove(it.uuid)
        }

        if (players.size <= 1) {
            endGame()
            return
        }
        nextRound()
    }
}