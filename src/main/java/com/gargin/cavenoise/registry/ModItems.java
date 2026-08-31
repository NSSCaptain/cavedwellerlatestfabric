package com.gargin.cavenoise.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class ModItems {
    public static final SpawnEggItem CAVE_DWELLER_SPAWN_EGG;

    public ModItems() {
    }

    public static <I extends Item> I registerSpawnEggs(String name, I item) {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register((content) -> content.accept(item));
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("cavenoise", name), item);
    }

    static {
        CAVE_DWELLER_SPAWN_EGG = registerSpawnEggs("cave_dweller_spawn_egg", new SpawnEggItem(ModEntityTypes.CAVEDWELLER, 2039583, 855309, new Item.Properties()));
    }
}