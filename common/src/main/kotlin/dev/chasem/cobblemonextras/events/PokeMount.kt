package dev.chasem.cobblemonextras.events

import com.cobblemon.mod.common.Cobblemon.storage
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand

class PokeMount {
    companion object {
        fun handleUseEntity(player: PlayerEntity, hand: Hand, pokemonEntity: PokemonEntity): ActionResult {
            val pokemon = pokemonEntity.pokemon

            var isPokemonInParty = false
            storage.getParty(player.uuid).forEach {
                if (it.uuid == pokemon.uuid) {
                    isPokemonInParty = true
                }
            }

            if (!isPokemonInParty || pokemon.friendship < 255) {
                return ActionResult.PASS
            }

            var fWater = false
            var fGround = false
            var fAir = false
            pokemon.moveSet.getMoves().forEach {
                if (it.name == "strength") {
                    fGround = true
                }

                if (it.name == "surf") {
                    fWater = true
                }

                if (it.name == "fly") {
                    fAir = true
                }
            }

            player.sendMessage(Text.literal("Riding"))
            player.startRiding(pokemonEntity)

            return ActionResult.PASS
        }
    }
}