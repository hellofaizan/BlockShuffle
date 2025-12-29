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
        var targetBlock: Material,
        var completed: Boolean = false
    )

    var state = GameState.WAITING
    var round = 1
    var players = mutableMapOf<UUID, PlayerData>()

    fun startGame() {
        state = GameState.RUNNING
        round = 1
        players.clear()

        if (Bukkit.getOnlinePlayers().size == 1) {
            Bukkit.broadcastMessage("Solo Mode Activated")
        }

        Bukkit.broadcastMessage("§a\uD83D\uDFE2 Block Shuffle Started!")

        for (player in Bukkit.getOnlinePlayers()) {
            players[player.uniqueId] = PlayerData(
                player.uniqueId,
                randomBlock()
            )
            player.sendMessage("§eYour block: §6${players[player.uniqueId]!!.targetBlock}")
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

        // SOLO
        if (players.size == 1) {
            val player = players.values.first()
            if(player.completed) {
                endGame()
            } else {
                Bukkit.getPlayer(player.uuid)?.sendMessage("You failed! Try again.")
                state = GameState.ENDED
            }
            return
        }

        // MULTIPLAYER
        if (players.size > 1) {
            nextRound()
            return
        }
        nextRound()
    }

    fun nextRound() {
        round++
        Bukkit.broadcastMessage("§e\uD83D\uDD01 Round $round!")

        players.values.forEach {
            it.completed = false
            it.targetBlock = randomBlock()
            Bukkit.getPlayer(it.uuid)?.sendMessage("§aNew block: §6 ${it.targetBlock}")
        }

        startTimer()
    }

    fun endGame() {
        val winner = players.values.firstOrNull()
        val player = Bukkit.getPlayer(winner?.uuid ?: return)

        Bukkit.broadcastMessage("§6${player?.name} wins Block Shuffle!")
        state = GameState.ENDED
    }

    private fun randomBlock(): Material {
        return Material.entries.filter {
            it.isBlock &&
                    it.isSolid &&
                    !it.name.contains("AIR") &&
                    !it.name.contains("WATER") &&
                    !it.name.contains("LAVA")
        }.random()
    }
}