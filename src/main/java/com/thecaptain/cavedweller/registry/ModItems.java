package com.thecaptain.cavedweller.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModItems {
    public static final Item CAVE_DWELLER_SPAWN_EGG;

    public ModItems() {
    }

    public static <I extends Item> I registerItem(String name, I item) {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register((content) -> content.accept(item));
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("cave_dweller", name), item);
    }

    static {
        CAVE_DWELLER_SPAWN_EGG = registerItem("cave_dweller_spawn_egg", new SpawnEggItem(
                ModEntityTypes.CAVEDWELLER,
                0x98863F, // Primary Background Color
                0x5B2E35, // Secondary Spots Color
                new FabricItemSettings()
        ));
    }
}