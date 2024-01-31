package dev.chasem.cobblemonextras.fabric.events

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import dev.chasem.cobblemonextras.events.PokeMount
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.world.World

class UseEntityHandler : UseEntityCallback{

    private var executed = false
    override fun interact(
        player: PlayerEntity?,
        world: World?,
        hand: Hand?,
        entity: Entity?,
        hitResult: EntityHitResult?
    ): ActionResult {
        if (player == null || world == null || hand == null || entity == null) {
            return ActionResult.PASS
        }

        if (world.isClient()) {
            return ActionResult.PASS
        }

        if (executed) {
            executed = false
            return ActionResult.PASS
        }

        executed = true

        try {
            val pokemonEntity = entity as PokemonEntity
            val pokemon = pokemonEntity.pokemon

            if (pokemon.heldItem().item != Items.SADDLE) {
                return ActionResult.PASS
            }

            return PokeMount.handleUseEntity(player, hand, entity)
        }
        catch (e: Exception) {}

        return ActionResult.PASS
    }
}