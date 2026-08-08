package com.gargin.cavenoise;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final String MOD_ID = "cavedweller";

    public static final EntityType<CaveDwellerEntity> CAVE_DWELLER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(MOD_ID, "cave_dweller"),
            FabricEntityTypeBuilder.<CaveDwellerEntity>create(MobCategory.MONSTER, CaveDwellerEntity::new)
                    .dimensions(EntityDimensions.scalable(0.6F, 2.9F))
                    .build()
    );

    public static void registerModEntities() {
    }
}