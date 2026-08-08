package com.gargin.cavenoise.entity;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import com.gargin.cavenoise.CaveNoise;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    public static final EntityType<CaveDwellerEntity> CAVE_DWELLER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(CaveNoise.MODID, "cave_dweller"),
            EntityType.Builder.of(CaveDwellerEntity::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.7F)
                    .build(new ResourceLocation(CaveNoise.MODID, "cave_dweller").toString())
    );

    public static void register() {}
}