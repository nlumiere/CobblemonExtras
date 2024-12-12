package dev.chasem.cobblemonextras.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor
import com.cobblemon.mod.common.battles.ai.RandomBattleAI
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.party
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.server.network.ServerPlayerEntity
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

            val npcParty = List<BattlePokemon>(6) {
                val pkmn = Pokemon()
                // Only kids can have legies
                if (!villagerEntity.isBaby && (pkmn.isLegendary() || pkmn.isMythical()) && battleLevel != BattleLevel.EXTREME) {
                    pkmn.currentHealth = 0
                }
                if (battleLevel == BattleLevel.EASY) {
                    pkmn.level = 1.coerceAtLeast(Random.nextInt(playerAceLevel - 10, playerAceLevel - 5))
                }
                else if (battleLevel == BattleLevel.MEDIUM) {
                    pkmn.level = 1.coerceAtLeast(Random.nextInt(playerAceLevel - 5, playerAceLevel - 1))
                }
                else if (battleLevel == BattleLevel.DIFFICULT) {
                    pkmn.level = 100.coerceAtMost(Random.nextInt(playerAceLevel, playerAceLevel + 2))
                }
                else if (battleLevel == BattleLevel.EXTREME){
                    pkmn.level = 100
                }

//                if (battleLevel != BattleLevel.EXTREME) {
//                    val preEvolution = pkmn.preEvolution
//                    if (preEvolution != null) {
//                        pkmn.species = preEvolution.species
//                    }
//                }

                BattlePokemon.safeCopyOf(pkmn.initialize())
            }
            val npcActor = TrainerBattleActor("Villager", villagerEntity.uuid, npcParty, RandomBattleAI())

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