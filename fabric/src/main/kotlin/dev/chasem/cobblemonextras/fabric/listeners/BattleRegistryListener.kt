package dev.chasem.cobblemonextras.fabric.listeners

import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.platform.events.PlatformEvents
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import java.util.HashMap
import java.util.UUID

class BattleRegistryListener {
    companion object {
        private val playerNPCBattleMap = HashMap<UUID, Pair<UUID, Item>?>()
        private var tickCount = 0
        private val rewardMap = HashMap<Item, HashSet<Item>>()

        fun initialize() {
            PlatformEvents.SERVER_TICK_POST.subscribe { tick(it.server) }

            rewardMap[Items.IRON_INGOT] = HashSet()
            rewardMap[Items.EMERALD] = HashSet()
            rewardMap[Items.DIAMOND] = HashSet()
            rewardMap[Items.NETHERITE_INGOT] = HashSet()

//            rewardMap[Items.IRON_INGOT].
        }

        fun put(player: PlayerEntity, npc: VillagerEntity, item: Item) {
            playerNPCBattleMap[player.uuid] = Pair(npc.uuid, item)
        }

        private fun removeIfExists(uuid: UUID) {
            if (playerNPCBattleMap.containsKey(uuid) && playerNPCBattleMap[uuid] != null) {
                playerNPCBattleMap[uuid] = null
            }
        }

        private fun tick(server: MinecraftServer) {
            tickCount++
            if (tickCount % 30 != 0) {
                return
            }
            else {
                tickCount = 0
            }

            // Nightmare
            playerNPCBattleMap.forEach { it ->
                val battle = BattleRegistry.getBattleByParticipatingPlayerId(it.key)
                val player = server.playerManager.getPlayer(it.key) ?: return

                battle?.showdownMessages?.forEach { m ->
                    if (m.startsWith("end")) {
                        val parsed = m.substringAfter("\"winner\":\"")
                        val uid = parsed.substringBefore("\"")
                        val parsed2 = parsed.substringAfter("p1\":\"")
                        val uid2 = parsed2.substringBefore("\"")
                        if (uid == uid2) {
                            val item = it.value?.second
                            if (item != null) {
                                removeIfExists(it.key)
                                player.giveItemStack(ItemStack(item, 2))
                                player.sendMessage(Text.literal("Congratulations! You have won 2 " + item.toString() + "s!"))
                            }
                        }
                    }
                }
            }
        }
    }
}