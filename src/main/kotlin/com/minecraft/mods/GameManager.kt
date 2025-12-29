package com.minecraft.mods

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask
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
    private var currentTimer: BukkitTask? = null

    private val config = BlockShuffle.instance.config

    fun startGame() {
        val minPlayers = config.getInt("min-players", 1)
        
        if (Bukkit.getOnlinePlayers().size < minPlayers) {
            Bukkit.broadcastMessage("§cNot enough players to start Block Shuffle. Minimum required: $minPlayers")
            state = GameState.ENDED
            return
        }

        state = GameState.RUNNING
        round = 1
        players.clear()

        Bukkit.broadcastMessage("§a\uD83D\uDFE2 Block Shuffle started!")

        for (player in Bukkit.getOnlinePlayers()) {
            players[player.uniqueId] = PlayerData(
                player.uniqueId,
                getWeightedBlock()
            )
            player.sendMessage("§eYour block: §6${players[player.uniqueId]!!.targetBlock}")
            // beacon activate sound at the start of game
            player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f)
        }
        startTimer()
    }

    fun startTimer() {
        currentTimer?.cancel()
        
        var timeLeft = config.getInt("round-time", 300)
        currentTimer = Bukkit.getScheduler().runTaskTimer(BlockShuffle.instance, Runnable {
            if (state != GameState.RUNNING) {
                currentTimer?.cancel()
                return@Runnable
            }
            checkActivePlayers()

            if (timeLeft <= 0) {
                currentTimer?.cancel()
                currentTimer = null
                endRound()
                return@Runnable
            }
            timeLeft--
        }, 0L, 20L)
    }

    private fun checkActivePlayers() {
        val offlinePlayers = players.keys.filter { Bukkit.getPlayer(it) == null }
        offlinePlayers.forEach { uUID ->
            players.remove(uUID)
        }

        if (players.isEmpty() && state == GameState.RUNNING) {
            Bukkit.broadcastMessage("§cGame ended - all players have left the game.")
            state = GameState.ENDED
            currentTimer?.cancel()
            currentTimer = null
        }
    }

    fun endRound() {
        val failed = players.values.filter { !it.completed }

        failed.forEach {
            val p = Bukkit.getPlayer(it.uuid)
            p?.sendMessage("§cYou failed this round")
            players.remove(it.uuid)
        }

        if (players.isEmpty()) {
            Bukkit.broadcastMessage("§cAll players failed! Game ended.")
            state = GameState.ENDED
            currentTimer?.cancel()
            currentTimer = null
            return
        }

        // SOLO
        if (players.size == 1) {
            val player = players.values.first()
            if(player.completed) {
                nextRound()
            } else {
                Bukkit.getPlayer(player.uuid)?.sendMessage("You failed! Try again.")
                state = GameState.ENDED
                currentTimer?.cancel()
                currentTimer = null
            }
            return
        }

        nextRound()
    }

    fun nextRound() {
        round++
        Bukkit.broadcastMessage("§e\uD83D\uDD01 Round $round!")

        players.values.forEach {
            it.completed = false
            it.targetBlock = getWeightedBlock()
            val player = Bukkit.getPlayer(it.uuid)
            player?.sendMessage("§aNew block: §6 ${it.targetBlock}")
            player?.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f)
        }

        startTimer()
    }

    fun endGame() {
        val winner = players.values.firstOrNull() ?: return
        val player = Bukkit.getPlayer(winner.uuid) ?: return

        Bukkit.broadcastMessage("§6${player.name} wins Block Shuffle!")
        state = GameState.ENDED
        currentTimer?.cancel()
        currentTimer = null
    }

    fun quitGame(playerUuid: UUID) {
        if (state != GameState.RUNNING) return

        val player = Bukkit.getPlayer(playerUuid) ?: return
        val playerData = players.remove(playerUuid) ?: return

        Bukkit.broadcastMessage("§e${player.name} has left this Block Shuffle game.")

        // If no players left, end the game
        if (players.isEmpty()) {
            Bukkit.broadcastMessage("§cGame ended - no players remaining.")
            state = GameState.ENDED
            currentTimer?.cancel()
            currentTimer = null
            return
        }

        // In solo mode, end the game
        if (players.size == 1) {
            val remainingPlayer = Bukkit.getPlayer(players.values.first().uuid)
            remainingPlayer?.sendMessage("§aYou are the last player remaining!")
            Bukkit.broadcastMessage("§a${remainingPlayer?.name} is the last player remaining!")
            state = GameState.ENDED
            currentTimer?.cancel()
            currentTimer = null
            return
        }

        // Multiple players still in game - continue
        Bukkit.broadcastMessage("§e${player.name} has quit the game. ${players.size} players remaining.")
    }

    fun handlePlayerDisconnect(playerUuid: UUID) {
        if (state != GameState.RUNNING) return

        val playerData = players.remove(playerUuid) ?: return
        val playerName = Bukkit.getOfflinePlayer(playerUuid).name ?: "Unknown"

        Bukkit.broadcastMessage("§e$playerName has disconnected from the game.")

        // If no players left, end the game
        if (players.isEmpty()) {
            Bukkit.broadcastMessage("§cGame ended - all players have left.")
            state = GameState.ENDED
            currentTimer?.cancel()
            currentTimer = null
            return
        }

        // In solo mode, end the game
        if (players.size == 1) {
            val remainingPlayer = Bukkit.getPlayer(players.values.first().uuid)
            remainingPlayer?.sendMessage("§aYou are the last player remaining!")
            Bukkit.broadcastMessage("§a${remainingPlayer?.name} is the last player remaining!")
            state = GameState.ENDED
            currentTimer?.cancel()
            currentTimer = null
            return
        }

        Bukkit.broadcastMessage("§e$playerName has disconnected. ${players.size} players remaining.")
    }

    fun cleanup() {
        currentTimer?.cancel()
        currentTimer = null
        state = GameState.ENDED
        round = 1
        players.clear()

    }

    private fun isAllowed(material: Material) : Boolean {
        val config = BlockShuffle.instance.config

        if (config.getStringList("blacklist").contains(material.name)) return false

        val allowSection = config.getConfigurationSection("allow") ?: return true
        val allowNether = allowSection.getBoolean("nether", true)
        val allowEnd = allowSection.getBoolean("end", false)

        if (!allowNether && material.name.contains("NETHER", ignoreCase = true)) return false

        if (!allowEnd && material.name.contains("END", ignoreCase = true) && !material.name.contains("END_ROD")) return false

        if (!material.isBlock || !material.isSolid) return false

        if (material == Material.AIR ||
            material == Material.WATER ||
            material == Material.LAVA ||
            material.name.endsWith("_AIR") ||
            material.name.contains("WATER") ||
            material.name.contains("LAVA")) return false
        return true
    }

    private fun getBlockWeight(material: Material): Int {
        val config = BlockShuffle.instance.config
        val categoryWeights = config.getConfigurationSection("weight-categories") ?: return 5 // default weight
        
        val name = material.name.uppercase()
        
        // Check for individual block override first
        val individualWeights = config.getConfigurationSection("weights")
        if (individualWeights != null && individualWeights.contains(material.name)) {
            return individualWeights.getInt(material.name, 5)
        }
        
        // Categorize blocks by name patterns and assign weights
        // Order matters: check more specific patterns first
        
        // Common natural blocks (exact matches only) - check first
        if (name == "STONE" || name == "COBBLESTONE" || name == "DIRT" || name == "GRASS_BLOCK") {
            return categoryWeights.getInt("common-blocks", 10)
        }
        
        // Ultra rare blocks (emerald, ancient debris, etc.)
        if (name.contains("EMERALD") || name.contains("ANCIENT_DEBRIS") || name.contains("NETHERITE")) {
            return categoryWeights.getInt("ultra-rare", 1)
        }
        
        // Rare ores (diamond, lapis, redstone)
        if (name.contains("DIAMOND") || name.contains("LAPIS") || name.contains("REDSTONE_ORE")) {
            return categoryWeights.getInt("rare-ores", 2)
        }
        
        // Uncommon ores (iron, copper, gold)
        if (name.contains("IRON_ORE") || name.contains("COPPER_ORE") || name.contains("GOLD_ORE")) {
            return categoryWeights.getInt("uncommon-ores", 4)
        }
        
        // Common ores (coal)
        if (name.contains("COAL_ORE")) {
            return categoryWeights.getInt("common-ores", 6)
        }
        
        // Nether exclusive blocks (check before end blocks to avoid conflicts)
        if (name.contains("NETHER") && !name.contains("NETHERITE")) {
            return categoryWeights.getInt("nether-blocks", 5)
        }
        
        // End exclusive blocks
        if (name.contains("END") && !name.contains("END_ROD")) {
            return categoryWeights.getInt("end-blocks", 3)
        }
        
        // Decorative blocks (glass, stained, etc.)
        if (name.contains("GLASS") || name.contains("STAINED") || name.contains("WOOL") || name.contains("CARPET")) {
            return categoryWeights.getInt("decorative", 7)
        }
        
        // Building blocks (stone variants, wood, etc.) - check last as it's broad
        // Note: STONE, DIRT, GRASS already handled above, so this catches variants
        if (name.contains("STONE") || name.contains("BRICK") || name.contains("CONCRETE") || 
            name.contains("TERRACOTTA") || name.contains("LOG") || name.contains("PLANK") ||
            name.contains("WOOD") || name.contains("SAND") || name.contains("GRAVEL") || 
            name.contains("CLAY") || (name.contains("DIRT") && name != "DIRT") ||
            (name.contains("GRASS") && name != "GRASS_BLOCK")) {
            return categoryWeights.getInt("building-blocks", 8)
        }
        
        // Default weight for unclassified blocks
        return categoryWeights.getInt("default", 5)
    }
    
    private fun getWeightedBlock(): Material {
        val config = BlockShuffle.instance.config
        val reduction = config.getDouble("difficulty.weight-reduction-per-round", 0.15)

        // Get all allowed blocks
        val allAllowedBlocks = Material.entries.filter { isAllowed(it) }
        
        if (allAllowedBlocks.isEmpty()) {
            BlockShuffle.instance.logger.warning("No allowed blocks found! Using STONE as fallback")
            return Material.STONE
        }

        val weightedList = mutableListOf<Material>()

        // Build weighted list from all allowed blocks
        for (material in allAllowedBlocks) {
            val baseWeight = getBlockWeight(material)
            // Calculate scaled weight: easier blocks (higher weight) get reduced more per round
            // So harder blocks (lower weight) become more common as rounds progress
            val scaledWeight = (baseWeight - (round - 1) * reduction).toInt().coerceAtLeast(1)
            
            if (scaledWeight > 0) {
                repeat(scaledWeight) {
                    weightedList.add(material)
                }
            }
        }

        // Fallback if no valid blocks found (shouldn't happen, but safety check)
        if (weightedList.isEmpty()) {
            BlockShuffle.instance.logger.warning("No valid weighted blocks found! Using first allowed block")
            return allAllowedBlocks.firstOrNull() ?: Material.STONE
        }

        return weightedList.random()
    }

}