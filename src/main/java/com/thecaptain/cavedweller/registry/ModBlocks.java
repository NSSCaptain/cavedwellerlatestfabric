package com.thecaptain.cavedweller.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ModBlocks {
    private static Block cachedTorch;
    private static Block cachedWallTorch;

    public static Block getBurntOutTorch() {
        if (cachedTorch == null) {
            cachedTorch = BuiltInRegistries.BLOCK.get(new ResourceLocation("cavedweller", "burnt_out_torch"));

            if (cachedTorch == Blocks.AIR) {
                System.out.println("[Cave Dweller ERROR] 'cavedweller:burnt_out_torch.json' is missing from registry!");
                return Blocks.TORCH;
            }
        }
        return cachedTorch;
    }

    public static Block getBurntOutWallTorch() {
        if (cachedWallTorch == null) {
            cachedWallTorch = BuiltInRegistries.BLOCK.get(new ResourceLocation("cavedweller", "burnt_out_wall_torch"));

            if (cachedWallTorch == Blocks.AIR) {
                System.out.println("[Cave Dweller ERROR] 'cavedweller:burnt_out_wall_torch' is missing from registry!");
                return Blocks.WALL_TORCH;
            }
        }
        return cachedWallTorch;
    }
}
