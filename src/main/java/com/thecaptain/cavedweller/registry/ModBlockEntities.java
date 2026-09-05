package com.thecaptain.cavedweller.registry;

import com.thecaptain.cavedweller.block.entity.BurntOutTorchBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<BurntOutTorchBlockEntity> BURNT_OUT_TORCH_ENTITY;

    public static void registerBlockEntities() {
        BURNT_OUT_TORCH_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                new ResourceLocation("cavedweller", "burnt_out_torch_entity"),
                BlockEntityType.Builder.of(
                        BurntOutTorchBlockEntity::new,
                        ModBlocks.getBurntOutTorch(),
                        ModBlocks.getBurntOutWallTorch()
                ).build(null)
        );
    }
}