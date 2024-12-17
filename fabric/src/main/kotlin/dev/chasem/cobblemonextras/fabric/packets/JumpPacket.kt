import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import dev.chasem.cobblemonextras.events.PokeMount
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.entity.Entity

object JumpPacket {
    val PACKET_ID = Identifier("cobblemonextras", "jump_packet")

    // Send the packet to the server
    fun sendToServer() {
        val buf = PacketByteBufs.create()
        ClientPlayNetworking.send(PACKET_ID, buf)
    }

    // Register the packet handler on the server
    fun registerServerHandler() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID) { server, player, _, _, _ ->
            server.execute {
                handleJumpRequest(player)
            }
        }
    }

    // Handle the jump request on the server
    private fun handleJumpRequest(player: ServerPlayerEntity) {
        val entity: Entity? = player.vehicle
        try {
            val pokemonEntity = entity as PokemonEntity
            val flying = pokemonEntity.pokemon.types.contains(ElementalTypes.FLYING)
            val water = pokemonEntity.pokemon.types.contains(ElementalTypes.WATER)
            if (entity.isOnGround || flying || (water && entity.isSubmergedInWater)) {
                val existingVelocity = entity.velocity
                entity.setVelocity(existingVelocity.x, .5, existingVelocity.z)
            }
        }
        catch (e: Exception) {}
    }
}
