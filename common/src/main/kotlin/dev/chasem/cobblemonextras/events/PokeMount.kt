package dev.chasem.cobblemonextras.events

import com.cobblemon.mod.common.Cobblemon.storage
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import java.util.UUID

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

            if (!isPokemonInParty || pokemon.evolutions.toList().isNotEmpty() || pokemon.friendship < 100) {
                if (pokemon.evolutions.toList().isNotEmpty()) {
                    player.sendMessage(Text.literal("Pokemon must be fully evolved to ride."))
                }

                if (pokemon.friendship < 100) {
                    player.sendMessage(Text.literal("Your Pokemon doesn't like you enough to ride"))
                }
                return ActionResult.PASS
            }

            player.startRiding(pokemonEntity)

            return ActionResult.PASS
        }

        fun logisticMapping(input: Double, minInput: Double = 0.0, maxInput: Double = 400.0, minOutput: Double = 0.0, maxOutput: Double = 2.0): Double {
            input.coerceAtLeast(0.0).coerceAtMost(maxInput)
            val k = 0.03
            val midpoint = (maxInput - minInput) / 2.0

            val logisticValue = 1 / (1 + Math.exp(-k * (input - midpoint)))

            return minOutput + (maxOutput - minOutput) * logisticValue
        }
    }
}