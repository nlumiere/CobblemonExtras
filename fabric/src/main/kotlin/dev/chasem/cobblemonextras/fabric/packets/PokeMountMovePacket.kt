package dev.chasem.cobblemonextras.fabric.packets

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import dev.chasem.cobblemonextras.events.PokeMount
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.MovementType
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

object PokeMountMovePacket {
    val PACKET_ID = Identifier("cobblemonextras", "pokemount_move_packet")

    // Send the packet to the server
    fun sendToServer() {
        val buf = PacketByteBufs.create()
        ClientPlayNetworking.send(PACKET_ID, buf)
    }

    // Register the packet handler on the server
    fun registerServerHandler() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID) { server, player, _, _, _ ->
            server.execute {
                handleMoveRequest(player)
            }
        }
    }

    // Handle the jump request on the server
    private fun handleMoveRequest(player: ServerPlayerEntity) {
        val entity: Entity? = player.vehicle
        try {
            val pokemonEntity = entity as PokemonEntity

            pokemonEntity.pitch = player.pitch
            pokemonEntity.yaw = player.yaw
            pokemonEntity.headYaw = player.headYaw
            val lookVec = player.getRotationVec(1.0f)
            val x = lookVec.x
            val z = lookVec.z
            var vector3: Vec3d = Vec3d(x, 0.0, z)
            vector3 = vector3.normalize()
            val yVelocity = pokemonEntity.velocity.y
            val existingVelocity = Vec3d(pokemonEntity.velocity.x, 0.0, pokemonEntity.velocity.z)
            val velocityStatMultiplier = PokeMount.getSpeedMultiplier(pokemonEntity)
            val newVelocity = (existingVelocity.add(vector3)).normalize().multiply(.4).multiply(velocityStatMultiplier)
            pokemonEntity.setVelocity(newVelocity.x, yVelocity, newVelocity.z)
        }
        catch (e: Exception) {}
    }
}
