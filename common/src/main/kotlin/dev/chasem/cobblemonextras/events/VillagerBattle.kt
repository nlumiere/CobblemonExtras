package dev.chasem.cobblemonextras.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor
import com.cobblemon.mod.common.battles.ai.RandomBattleAI
import com.cobblemon.mod.common.battles.ai.StrongBattleAI
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.evolution.requirements.LevelRequirement
import com.cobblemon.mod.common.util.party
import dev.chasem.cobblemonextras.ai.NaiveAI
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import kotlin.random.Random

class VillagerBattle {
    enum class BattleLevel {
        EASY,
        MEDIUM,
        DIFFICULT,
        EXTREME
    }

    companion object {
        fun startBattle(player: ServerPlayerEntity, villagerEntity: VillagerEntity, battleLevel: BattleLevel = BattleLevel.EASY): ActionResult {
            if (getBattleByParticipatingPlayer(player) != null) {
                return ActionResult.PASS
            }

            val party = Cobblemon.storage.getParty(player.uuid)
            val playerTeam = party.toBattleTeam()
            val playerActor = PlayerBattleActor(player.uuid, playerTeam)

            val playerAceLevel = player.party().maxOf { it -> it.level }

            var skill = 5
            val npcParty = List<BattlePokemon>(6) {
                var pkmn = Pokemon()
                // Only kids can have legies
                if (!villagerEntity.isBaby && (pkmn.isLegendary() || pkmn.isMythical()) && battleLevel != BattleLevel.EXTREME) {
                    pkmn.currentHealth = 0
                }
                if (battleLevel == BattleLevel.EASY) {
                    pkmn.level = 1.coerceAtLeast(Random.nextInt(playerAceLevel - 8, playerAceLevel - 3))
                }
                else if (battleLevel == BattleLevel.MEDIUM) {
                    pkmn.level = 1.coerceAtLeast(Random.nextInt(playerAceLevel - 2, playerAceLevel))
                }
                else if (battleLevel == BattleLevel.DIFFICULT) {
                    pkmn.level = 100.coerceAtMost(Random.nextInt(playerAceLevel, playerAceLevel + 4))
                }
                else if (battleLevel == BattleLevel.EXTREME){
                    pkmn.level = 100
                }

                pkmn = pkmn.initialize()
                val preEvolution = pkmn.preEvolution
                if (preEvolution != null && (battleLevel == BattleLevel.EASY || battleLevel == BattleLevel.MEDIUM)) {
                    val newPokemon = Pokemon()
                    newPokemon.level = pkmn.level
                    newPokemon.species = preEvolution.species
                    pkmn = newPokemon.initialize()
                }

                var evolutions = pkmn.evolutions
                var shouldStopNow = battleLevel == BattleLevel.EASY
                while (evolutions.toList().isNotEmpty() && !shouldStopNow) {
                    var meetsRequirements = true
                    evolutions.first().requirements.forEach {
                        if (it is LevelRequirement && !it.check(pkmn)) {
                            meetsRequirements = false
                        }
                    }
                    if (!meetsRequirements) {
                        break
                    }

                    pkmn.evolutions.first().evolutionMethod(pkmn)
                    evolutions = pkmn.evolutions
                    shouldStopNow = battleLevel == BattleLevel.MEDIUM
                }

                BattlePokemon.safeCopyOf(pkmn)
            }

            val npcActor = TrainerBattleActor("Villager", villagerEntity.uuid, npcParty, StrongBattleAI(skill))

//            val battleFormat = if (battleLevel == BattleLevel.EASY) BattleFormat.GEN_9_SINGLES else if (Random.nextInt() % 2 == 0) BattleFormat.GEN_9_SINGLES else BattleFormat.GEN_9_DOUBLES
            val battleFormat = BattleFormat.GEN_9_SINGLES
            var result = ActionResult.PASS
            BattleRegistry.startBattle(battleFormat, BattleSide(playerActor), BattleSide(npcActor)).ifSuccessful { it ->
                SuccessfulBattleStart(it)
                result = ActionResult.SUCCESS
            }
            // Really bemoaning the current nonexistence of onEndHandlers rn :/

            return result
        }
    }
}