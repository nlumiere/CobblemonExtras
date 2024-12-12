package dev.chasem.cobblemonextras.ai

import com.cobblemon.mod.common.api.battles.model.ai.BattleAI
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.moves.categories.DamageCategories
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import kotlin.random.Random

class NaiveAI : BattleAI {
    override fun choose(
        activeBattlePokemon: ActiveBattlePokemon,
        moveset: ShowdownMoveset?,
        forceSwitch: Boolean
    ): ShowdownActionResponse {
        if (forceSwitch || activeBattlePokemon.isGone()) {
            return switchRandomly(activeBattlePokemon)
        }

        if (moveset == null) {
            return PassActionResponse
        }

        var move = null
        val filteredMoves = moveset.moves
            .filter { it.canBeUsed() }
            .filter { it.mustBeUsed() || it.target.targetList(activeBattlePokemon)?.isEmpty() != true }

        if (activeBattlePokemon.battlePokemon == null) {
            return switchRandomly(activeBattlePokemon)
        }
        val activeSpeed = activeBattlePokemon.battlePokemon?.effectedPokemon?.getStat(Stats.SPEED)
            ?: activeBattlePokemon.battlePokemon!!.originalPokemon.speed
        activeBattlePokemon.getSide().getOppositeSide().actors.forEach {
            it.activePokemon.forEach { a ->
                val enemySpeed = a.battlePokemon?.effectedPokemon?.getStat(Stats.SPEED)
                if (enemySpeed != null && enemySpeed >= activeSpeed && Random.nextInt(2) % 2 == 0) {
                    return switchRandomly(activeBattlePokemon)
                }
            }
        }

        val mostDamagingOption = activeBattlePokemon.actor.getSide().getOppositeSide().activePokemon.first().battlePokemon?.let {
            mostDamagePossible(filteredMoves, activeBattlePokemon,
                it, "Hail")
        }
        if (mostDamagingOption!!.first > .5 && mostDamagingOption.second != null) {
            return MoveActionResponse(mostDamagingOption.second!!.move.lowercase())
        }

        // Travesty
        val statusMoves = moveset.moves.filter {
            it.move.lowercase().contains("spore") ||
            it.move.lowercase().contains("swords") ||
            it.move.lowercase().contains("thunder") ||
            it.move.lowercase().contains("recover") ||
            it.move.lowercase().contains("protect") ||
            it.move.lowercase().contains("baneful") ||
            it.move.lowercase().contains("bulwark") ||
            it.move.lowercase().contains("trick") ||
            it.move.lowercase().contains("agility") ||
            it.move.lowercase().contains("calm") ||
            it.move.lowercase().contains("quiver") ||
            it.move.lowercase().contains("stealth") ||
            it.move.lowercase().contains("iron") ||
            it.move.lowercase().contains("toxic") ||
            it.move.lowercase().contains("wisp")
        }

        if (statusMoves.isNotEmpty()) {
            val chosenMove = statusMoves.randomOrNull()
            if (chosenMove != null) {
                return MoveActionResponse(chosenMove.move.lowercase())
            }
        }

        return switchRandomly(activeBattlePokemon)
    }

    private fun switchRandomly(activeBattlePokemon: ActiveBattlePokemon): ShowdownActionResponse {
        val switchTo = activeBattlePokemon.actor.pokemonList.filter { it.canBeSentOut() }.randomOrNull()
            ?: return MoveActionResponse("struggle")
        switchTo.willBeSwitchedIn = true
        return SwitchActionResponse(switchTo.uuid)
    }

    private fun mostDamagePossible(moveset: List<InBattleMove>, attacker: ActiveBattlePokemon, defender: BattlePokemon, currentWeather: String?): Pair<Double, InBattleMove?> {
        var damage = 0.0
        var move: InBattleMove? = null
        moveset.forEach {
            val newDamage = calculateDamage(it, attacker.battlePokemon!!, defender, currentWeather)/defender.effectedPokemon.currentHealth
            if (newDamage > damage) {
                damage = newDamage
                move = it
            }
        }
        return Pair(damage, move)
    }

// Rest of file is adapted from official Cobblemon mod, which has not been released yet as of 1.5.2.
// See the original open source functions as part of StrongBattleAI at https://gitlab.com/cable-mc/cobblemon
    private fun calculateDamage(move: InBattleMove, mon: BattlePokemon, opponent: BattlePokemon, currentWeather: String?): Double {
        val moveData = Moves.getByName(move.id)

        val physicalRatio = statEstimationActive(mon, Stats.ATTACK) / statEstimationActive(opponent, Stats.DEFENCE)
        val specialRatio = statEstimationActive(mon, Stats.SPECIAL_ATTACK) / statEstimationActive(opponent, Stats.SPECIAL_DEFENCE)

        // Attempt at better estimation
        val movePower = moveData!!.power
        val pokemonLevel = mon.effectedPokemon.level
        val statRatio = if (moveData.damageCategory == DamageCategories.PHYSICAL) physicalRatio else specialRatio

        val STAB = when {
            moveData.elementalType in mon.effectedPokemon.types && mon.effectedPokemon.ability.name == "adaptability" -> 2.0
            moveData.elementalType in mon.effectedPokemon.types -> 1.5
            else -> 1.0
        }
        val weather = when {
            // Sunny Weather
            currentWeather == "sunny" && (moveData.elementalType == ElementalTypes.FIRE || moveData.name == "hydrosteam") -> 1.5
            currentWeather == "sunny" && moveData.elementalType == ElementalTypes.WATER && moveData.name != "hydrosteam" -> 0.5

            // Rainy Weather
            currentWeather == "raining" && moveData.elementalType == ElementalTypes.WATER-> 1.5
            currentWeather == "raining" && moveData.elementalType == ElementalTypes.FIRE-> 0.5

            // Add other cases below for weather

            else -> 1.0
        }
        val damageTypeMultiplier = moveDamageMultiplier(move.id, opponent)
        val burn = when {
            opponent.effectedPokemon.status?.status?.showdownName == "burn" && moveData.damageCategory == DamageCategories.PHYSICAL -> 0.5
            else -> 1.0
        }
        //val hitsExpected = expectedHits(Moves.getByName(move.id)!!) // todo fix this as it has null issues

        var damage = (((((2 * pokemonLevel) / 5 ) + 2) * movePower * statRatio) / 50 + 2)
        damage *= weather
        damage *= STAB
        damage *= damageTypeMultiplier
        damage *= burn
        //damage *= hitsExpected

        return damage
    }

    private fun statEstimationActive(mon: BattlePokemon, stat: Stat): Double {
        val boost = mon.statChanges[stat]

        val actualBoost = if (boost!! > 1) {
            (2 + boost) / 2.0
        } else {
            2 / (2.0 - boost)
        }
        val baseStat = mon.effectedPokemon.getStat(stat)
        return ((2 * baseStat + 31) + 5) * actualBoost
    }

    private fun moveDamageMultiplier(moveID: String, defender: BattlePokemon): Double {
        val move = Moves.getByName(moveID)
        // repeat the list building for each entry in the list
        var typeList = mutableListOf<ElementalType>()

        defender.effectedPokemon.types.forEach {
            ElementalTypes.get(it.name.lowercase())?.let { it1 -> typeList.add(it1) }
        }

        val defenderTypes = typeList // set the type list of the current defender
        var multiplier = 1.0

        for (defenderType in defenderTypes)
            multiplier *= (getDamageMultiplier(move!!.elementalType, defenderType) ?: 1.0)

        return multiplier
    }

    fun getDamageMultiplier(attackerType: ElementalType, defenderType: ElementalType): Double {
        return typeEffectiveness[attackerType]?.get(defenderType) ?: 1.0

    }

    val typeEffectiveness: Map<ElementalType, Map<ElementalType, Double>> = mapOf(
        ElementalTypes.NORMAL to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 0.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.FIRE to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
            ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 2.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 2.0, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.WATER to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 2.0, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 0.5,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 2.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.ELECTRIC to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 2.0, ElementalTypes.ELECTRIC to 0.5, ElementalTypes.GRASS to 0.5,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 0.0, ElementalTypes.FLYING to 2.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.GRASS to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 2.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 0.5,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 2.0, ElementalTypes.FLYING to 0.5,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 0.5, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 0.5,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.ICE to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
            ElementalTypes.ICE to 0.5, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 2.0, ElementalTypes.FLYING to 2.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 2.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.FIGHTING to mapOf(
            ElementalTypes.NORMAL to 2.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 0.5,
            ElementalTypes.PSYCHIC to 0.5, ElementalTypes.BUG to 0.5, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 0.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 2.0, ElementalTypes.STEEL to 2.0, ElementalTypes.FAIRY to 0.5
        ),
        ElementalTypes.POISON to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 0.5, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 0.5, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.0, ElementalTypes.FAIRY to 2.0
        ),
        ElementalTypes.GROUND to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 2.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 2.0, ElementalTypes.GRASS to 0.5,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 2.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 0.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 0.5, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 2.0, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.FLYING to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 0.5, ElementalTypes.GRASS to 2.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 2.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 2.0, ElementalTypes.ROCK to 0.5, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.PSYCHIC to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 2.0, ElementalTypes.POISON to 2.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 0.5, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 0.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.BUG to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 2.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 0.5, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 0.5,
            ElementalTypes.PSYCHIC to 2.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 0.5, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 2.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 0.5
        ),
        ElementalTypes.ROCK to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 2.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 0.5, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 0.5, ElementalTypes.FLYING to 2.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 2.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.GHOST to mapOf(
            ElementalTypes.NORMAL to 0.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 2.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 2.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 0.5, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 1.0
        ),
        ElementalTypes.DRAGON to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 2.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 0.0
        ),
        ElementalTypes.DARK to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 1.0, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 0.5, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 2.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 2.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 0.5, ElementalTypes.STEEL to 1.0, ElementalTypes.FAIRY to 0.5
        ),
        ElementalTypes.STEEL to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 0.5, ElementalTypes.ELECTRIC to 0.5, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 2.0, ElementalTypes.FIGHTING to 1.0, ElementalTypes.POISON to 1.0, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 2.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 1.0,
            ElementalTypes.DARK to 1.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 2.0
        ),
        ElementalTypes.FAIRY to mapOf(
            ElementalTypes.NORMAL to 1.0, ElementalTypes.FIRE to 0.5, ElementalTypes.WATER to 1.0, ElementalTypes.ELECTRIC to 1.0, ElementalTypes.GRASS to 1.0,
            ElementalTypes.ICE to 1.0, ElementalTypes.FIGHTING to 2.0, ElementalTypes.POISON to 0.5, ElementalTypes.GROUND to 1.0, ElementalTypes.FLYING to 1.0,
            ElementalTypes.PSYCHIC to 1.0, ElementalTypes.BUG to 1.0, ElementalTypes.ROCK to 1.0, ElementalTypes.GHOST to 1.0, ElementalTypes.DRAGON to 2.0,
            ElementalTypes.DARK to 2.0, ElementalTypes.STEEL to 0.5, ElementalTypes.FAIRY to 1.0
        )
    )
}