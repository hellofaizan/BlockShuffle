package com.minecraft.mods

import org.bukkit.plugin.java.JavaPlugin

class BlockShuffle : JavaPlugin() {

    companion object {
        lateinit var instance: BlockShuffle
    }

    override fun onEnable() {
        // Plugin startup logic
        instance = this

        server.pluginManager.registerEvents(PlayerListener(), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
