package dev.chasem.cobblemonextras.fabric

import com.cobblemon.mod.common.platform.events.PlatformEvents
import dev.chasem.cobblemonextras.CobblemonExtras
import dev.chasem.cobblemonextras.fabric.events.UseBlockHandler
import dev.chasem.cobblemonextras.fabric.events.UseEntityHandler
import dev.chasem.cobblemonextras.fabric.item.Programs
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback

class CobblemonFabric : ModInitializer {
    override fun onInitialize() {
        System.out.println("Fabric Mod init")
        CobblemonExtras.initialize();
        CommandRegistrationCallback.EVENT.register(CobblemonExtras::registerCommands)
        ServerLifecycleEvents.SERVER_STOPPING.register { CobblemonExtras.onShutdown() }
        Programs.registerItems()
        UseEntityCallback.EVENT.register(UseEntityHandler())
        UseBlockCallback.EVENT.register(UseBlockHandler())
    }
}