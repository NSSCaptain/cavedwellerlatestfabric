package com.gargin.cavenoise.item;

import com.gargin.cavenoise.CaveNoise;
import com.gargin.cavenoise.entity.ModEntityTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import java.util.function.Supplier;

public class ModItems {
    public static final Supplier<Item> WORM = registerItem("worm", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BABY_SPIDER = registerItem("baby_spider", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CAVE_DWELLER_SPAWN_EGG = registerItem("cave_dweller_spawn_egg", () -> new SpawnEggItem(ModEntityTypes.CAVE_DWELLER, 12895428, 790333, new Item.Properties()));

    private static Supplier<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        Item item = itemSupplier.get();
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(CaveNoise.MODID, name), item);
        return () -> item;
    }

    public static void register() {}
}
