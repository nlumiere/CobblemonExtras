package dev.chasem.cobblemonextras.ai

import com.cobblemon.mod.common.api.battles.model.ai.BattleAI
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon

class NaiveAI : BattleAI {
    override fun choose(
        activeBattlePokemon: ActiveBattlePokemon,
        moveset: ShowdownMoveset?,
        forceSwitch: Boolean
    ): ShowdownActionResponse {
//        if (forceSwitch || activeBattlePokemon.isGone()) {
//            val switchTo = activeBattlePokemon.actor.pokemonList.filter { it.canBeSentOut() }.randomOrNull()
//                ?: return DefaultActionResponse() //throw IllegalStateException("Need to switch but no Pokémon to switch to")
//            switchTo.willBeSwitchedIn = true
//            return SwitchActionResponse(switchTo.uuid)
//        }
//
//        if (moveset == null) {
//            return PassActionResponse
//        }
//
//        var move = null
//        val filteredMoves = moveset.moves
//            .filter { it.canBeUsed() }
//            .filter { it.mustBeUsed() || it.target.targetList(activeBattlePokemon)?.isEmpty() != true }
//
//        filteredMoves.forEach { it ->
//
//            }
        return MoveActionResponse("struggle")
    }
}