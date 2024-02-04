package dev.chasem.cobblemonextras.fabric.item

import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class Programs {
    companion object {
        val CHECKSPAWN: Item = registerItem("checkspawn", Item(FabricItemSettings()))

        private fun addToCreative(entries: FabricItemGroupEntries) {
            entries.add(CHECKSPAWN)
        }

        fun registerItems() {
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(Programs::addToCreative)
        }

        fun registerItem(itemName: String, item: Item): Item {
            return Registry.register(Registries.ITEM, Identifier("cobblemonextras", itemName), item)
        }
    }
}