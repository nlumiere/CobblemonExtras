package dev.chasem.cobblemonextras.fabric.listeners

import JumpPacket
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import dev.chasem.cobblemonextras.fabric.packets.PokeMountMovePacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.MovementType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

class PokeMountMovementListener : ClientModInitializer {
    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register {
            tick(it)
        }
    }

    fun tick(client: MinecraftClient) {
        val player = client.player ?: return

        if (player.vehicle == null) {
            return
        }

        try {
            player.vehicle as PokemonEntity
        } catch (e: Exception) {}

        val pokemonEntity = player.vehicle as PokemonEntity
        val flying = pokemonEntity.pokemon.types.contains(ElementalTypes.FLYING)
        val water = pokemonEntity.pokemon.types.contains(ElementalTypes.WATER)

        val jumpKey = client.options.jumpKey.isPressed
        val isJumpValid = pokemonEntity.isOnGround || flying || (water && pokemonEntity.isTouchingWater)
        if (jumpKey && isJumpValid) {
            JumpPacket.sendToServer()
        }

        if (client.options.forwardKey.isPressed) {
            PokeMountMovePacket.sendToServer()
        }
    }
}