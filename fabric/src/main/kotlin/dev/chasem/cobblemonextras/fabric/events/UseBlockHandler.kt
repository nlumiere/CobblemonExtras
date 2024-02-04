package dev.chasem.cobblemonextras.fabric.events

import com.cobblemon.mod.common.CobblemonItems
import dev.chasem.cobblemonextras.events.UseProgramOnPC
import dev.chasem.cobblemonextras.fabric.item.Programs
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.world.World

class UseBlockHandler : UseBlockCallback {

    private val PROGRAMS = setOf<Item>(Programs.CHECKSPAWN)
    override fun interact(player: PlayerEntity?, world: World?, hand: Hand?, hitResult: BlockHitResult?): ActionResult {
        if (player == null || hand == null || world == null || world.isClient() || hitResult == null) {
            return ActionResult.PASS
        }

        val heldItem = player.getStackInHand(hand).item

        if (world.getBlockState(hitResult.blockPos).block.asItem() == CobblemonItems.PC && heldItem in PROGRAMS) {
            return UseProgramOnPC.checkspawn(player)
        }

        return ActionResult.PASS
    }

}