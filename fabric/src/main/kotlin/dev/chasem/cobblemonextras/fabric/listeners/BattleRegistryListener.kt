package dev.chasem.cobblemonextras.fabric.listeners

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.platform.events.PlatformEvents
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import java.util.HashMap
import java.util.UUID

class BattleRegistryListener {
    companion object {
        private val playerNPCBattleMap = HashMap<UUID, Pair<UUID, Item>?>()
        private val villagerLockoutMap = HashMap<UUID, Int>()
        private var tickCount = 0
        private val rewardMap = HashMap<Item, HashSet<Item>>()

        fun initialize() {
            PlatformEvents.SERVER_TICK_POST.subscribe { tick(it.server) }

            rewardMap[Items.IRON_INGOT] = hashSetOf(
                CobblemonItems.HYPER_POTION,
                CobblemonItems.FULL_HEAL,
                CobblemonItems.REVIVE,
                CobblemonItems.CALCIUM,
                CobblemonItems.CARBOS,
                CobblemonItems.PROTEIN,
                CobblemonItems.HP_UP,
                CobblemonItems.ZINC,
                CobblemonItems.IRON,
                CobblemonItems.PP_UP,
                CobblemonItems.STICKY_BARB,
                CobblemonItems.POKE_BALL,
                CobblemonItems.GREAT_BALL
            )

            rewardMap[Items.EMERALD] = hashSetOf(
                CobblemonItems.MAX_POTION,
                CobblemonItems.FULL_RESTORE,
                CobblemonItems.MAX_REVIVE,
                CobblemonItems.PP_MAX,
                CobblemonItems.RARE_CANDY,
                CobblemonItems.ADAMANT_MINT,
                CobblemonItems.JOLLY_MINT,
                CobblemonItems.MODEST_MINT,
                CobblemonItems.TIMID_MINT,
                CobblemonItems.IMPISH_MINT,
                CobblemonItems.CALM_MINT,
                CobblemonItems.SERIOUS_MINT,
                CobblemonItems.BOLD_MINT,
                CobblemonItems.MILD_MINT,
                CobblemonItems.BRAVE_MINT,
                CobblemonItems.GENTLE_MINT,
                CobblemonItems.HASTY_MINT,
                CobblemonItems.CAREFUL_MINT,
                CobblemonItems.LAX_MINT,
                CobblemonItems.NAIVE_MINT,
                CobblemonItems.RELAXED_MINT,
                CobblemonItems.SASSY_MINT,
                CobblemonItems.QUIET_MINT,
                CobblemonItems.RASH_MINT,
                CobblemonItems.ARMOR_FOSSIL,
                CobblemonItems.CLAW_FOSSIL,
                CobblemonItems.DOME_FOSSIL,
                CobblemonItems.COVER_FOSSIL,
                CobblemonItems.HELIX_FOSSIL,
                CobblemonItems.JAW_FOSSIL,
                CobblemonItems.SKY_TUMBLESTONE,
                CobblemonItems.RELIC_COIN,
                CobblemonItems.LINK_CABLE,
                CobblemonItems.METAL_COAT,
                CobblemonItems.RAZOR_CLAW,
                CobblemonItems.RAZOR_FANG,
                CobblemonItems.DEEP_SEA_SCALE,
                CobblemonItems.DEEP_SEA_TOOTH,
                CobblemonItems.ABSORB_BULB,
                CobblemonItems.PROTECTOR,
                CobblemonItems.BLACK_SLUDGE,
                CobblemonItems.CHOICE_BAND,
                CobblemonItems.CHOICE_SPECS,
                CobblemonItems.EVERSTONE,
                CobblemonItems.GHOST_GEM,
                CobblemonItems.GRASS_GEM,
                CobblemonItems.GROUND_GEM,
                CobblemonItems.BUG_GEM,
                CobblemonItems.DARK_GEM,
                CobblemonItems.DRAGON_GEM,
                CobblemonItems.ELECTRIC_GEM,
                CobblemonItems.FAIRY_GEM,
                CobblemonItems.FIGHTING_GEM,
                CobblemonItems.FIRE_GEM,
                CobblemonItems.FLYING_GEM,
                CobblemonItems.ICE_GEM,
                CobblemonItems.NORMAL_GEM,
                CobblemonItems.POISON_GEM,
                CobblemonItems.WATER_GEM,
                CobblemonItems.ROCK_GEM,
                CobblemonItems.STEEL_GEM,
                CobblemonItems.PSYCHIC_GEM,
                CobblemonItems.ULTRA_BALL,
                CobblemonItems.DUSK_BALL,
                CobblemonItems.TIMER_BALL
            )

            rewardMap[Items.DIAMOND] = hashSetOf(
                CobblemonItems.OVAL_STONE,
                CobblemonItems.DRAGON_SCALE,
                CobblemonItems.DUBIOUS_DISC,
                CobblemonItems.PRISM_SCALE,
                CobblemonItems.AUSPICIOUS_ARMOR,
                CobblemonItems.MALICIOUS_ARMOR,
                CobblemonItems.RELIC_COIN_POUCH,
                CobblemonItems.ABILITY_SHIELD,
                CobblemonItems.CHOICE_SCARF,
                CobblemonItems.EVIOLITE,
                CobblemonItems.LIFE_ORB,
                CobblemonItems.LEFTOVERS,
                CobblemonItems.EXP_SHARE,
                CobblemonItems.LUCKY_EGG,
                CobblemonItems.SHELL_BELL,
                CobblemonItems.ABILITY_PATCH,
                CobblemonItems.ABILITY_CAPSULE,
                CobblemonItems.FAIRY_FEATHER,
                Items.NAUTILUS_SHELL,
                Items.PRISMARINE_SHARD,
                Items.PRISMARINE_CRYSTALS,
                Items.SHULKER_SHELL,
                Items.TRIDENT,
                Items.ECHO_SHARD
            )

            rewardMap[Items.NETHERITE_INGOT] = hashSetOf(
                Items.NETHER_STAR,
                CobblemonItems.MASTER_BALL,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.TOTEM_OF_UNDYING,
                Items.HEART_OF_THE_SEA
            )
        }

        fun put(player: PlayerEntity, npc: VillagerEntity, item: Item) {
            playerNPCBattleMap[player.uuid] = Pair(npc.uuid, item)
            villagerLockoutMap[npc.uuid] = 600*20
        }

        private fun removeIfExists(uuid: UUID) {
            if (playerNPCBattleMap.containsKey(uuid) && playerNPCBattleMap[uuid] != null) {
                playerNPCBattleMap[uuid] = null
            }
        }

        fun timeBeforeVillagerCanBattle(uuid: UUID): Int {
            if (!villagerLockoutMap.containsKey(uuid)) {
                return 0
            }

            return villagerLockoutMap[uuid]!!/20
        }

        private fun tick(server: MinecraftServer) {
            tickCount++
            if (tickCount % 40 != 0) {
                return
            }
            else {
                tickCount = 0
            }

            villagerLockoutMap.forEach { it ->
                if (it.value > 0) {
                    val newVal = (it.value - 40).coerceAtLeast(0)
                    villagerLockoutMap[it.key] = newVal
                }
            }

            // Nightmare
            playerNPCBattleMap.forEach { it ->
                val battle = BattleRegistry.getBattleByParticipatingPlayerId(it.key)
                val player = server.playerManager.getPlayer(it.key) ?: return

                if (battle == null) {
                    removeIfExists(it.key)
                    return
                }

                battle.showdownMessages.forEach { m ->
                    if (m.startsWith("end")) {
                        val parsed = m.substringAfter("\"winner\":\"")
                        val uid = parsed.substringBefore("\"")
                        val parsed2 = parsed.substringAfter("p1\":\"")
                        val uid2 = parsed2.substringBefore("\"")
                        if (uid == uid2) {
                            val item = it.value?.second
                            if (item != null) {
                                removeIfExists(it.key)
                                player.giveItemStack(ItemStack(item, 1))
                                val randomItem = rewardMap[item]!!.random()
                                player.giveItemStack(ItemStack(randomItem, 1))
                                player.sendMessage(Text.literal("You have been awarded your $item as well as a $randomItem!"))
                            }
                        }
                    }
                }
            }
        }
    }
}