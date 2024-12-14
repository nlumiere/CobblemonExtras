package dev.chasem.cobblemonextras.fabric.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.party
import dev.chasem.cobblemonextras.events.PokeMount
import dev.chasem.cobblemonextras.events.VillagerBattle
import dev.chasem.cobblemonextras.events.VillagerBattle.BattleLevel
import dev.chasem.cobblemonextras.fabric.listeners.BattleRegistryListener
import dev.chasem.cobblemonextras.fabric.listeners.BattleRegistryListener.Companion.timeBeforeVillagerCanBattle
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.village.VillagerProfession
import net.minecraft.village.VillagerType
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

        try {
            val party = Cobblemon.storage.getParty(player.uuid);
            val villagerEntity = entity as VillagerEntity
            var partyOut = false
            party.forEach { it ->
                if (it.entity != null) {
                    partyOut = true
                }
            }

            // Start trainer battle against villager
            val heldItemStack = player.getStackInHand(hand)
            val heldItem = heldItemStack.item
            var battleLevel = VillagerBattle.BattleLevel.EASY
            if (heldItem == Items.IRON_INGOT) {
            } else if (heldItem == Items.EMERALD) {
                battleLevel = VillagerBattle.BattleLevel.MEDIUM
            } else if (heldItem == Items.DIAMOND) {
                battleLevel = VillagerBattle.BattleLevel.DIFFICULT
            } else if (heldItem == Items.NETHERITE_INGOT) {
                battleLevel = VillagerBattle.BattleLevel.EXTREME
            } else if (partyOut){
                player.sendMessage(Text.literal("This villager can battle, but only will if there's a wager.\nEasy: Iron Ingot\nMedium: Emerald\nHard: Diamond\nExtreme: Netherite Ingot"))
                return ActionResult.PASS
            } else {
                return ActionResult.PASS
            }

            if (!partyOut) {
                player.sendMessage(Text.literal("This villager will battle you. Send out a Pokemon and try your wager again."))
                return ActionResult.PASS
            }

            // If villager is unemployed, they must be a pokemon trainer. I don't make the rules.
            if (villagerEntity.villagerData.profession == VillagerProfession.NONE || villagerEntity.villagerData.profession == null || villagerEntity.villagerData.profession == VillagerProfession.NITWIT) {
                val timeLeft = timeBeforeVillagerCanBattle(villagerEntity.uuid)
                if (timeLeft > 0) {
                    player.sendMessage(Text.literal("You must wait another $timeLeft seconds before battling with this villager again.").formatted(Formatting.RED))
                    return ActionResult.PASS
                }

                if (villagerEntity.villagerData.profession == VillagerProfession.NITWIT && (battleLevel == VillagerBattle.BattleLevel.DIFFICULT || battleLevel == VillagerBattle.BattleLevel.EXTREME)) {
                    player.sendMessage(Text.literal("This villager is a nitwit. Nitwits can only battle in Easy or Medium level battles. Try offering iron or emeralds.").formatted(Formatting.RED))
                    return ActionResult.PASS
                }

                val playerAceLevel = (player as ServerPlayerEntity).party().maxOf { it -> it.level }

                if (playerAceLevel < 40 && battleLevel == BattleLevel.DIFFICULT) {
                    player.sendMessage(Text.literal("Difficult battles unlocked at level 40.").formatted(Formatting.RED))
                    return ActionResult.PASS
                }

                heldItemStack.decrement(1)
                val result = VillagerBattle.startBattle(player as ServerPlayerEntity, villagerEntity, battleLevel)
                if (result == ActionResult.SUCCESS) {
                    BattleRegistryListener.put(player, villagerEntity, heldItem)
                }
            }
        }
        catch (e: Exception) {}

        return ActionResult.PASS
    }
}