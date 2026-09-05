package com.thecaptain.cavedweller;

import com.thecaptain.cavedweller.registry.ModBlocks;
import com.thecaptain.cavedweller.registry.ModEntityTypes;
import com.thecaptain.cavedweller.registry.ModItems;
import com.thecaptain.cavedweller.registry.ModSounds;
import com.thecaptain.cavedweller.util.config.CaveDwellerConfiguration;
import com.thecaptain.cavedweller.util.config.ModConfigModel;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

public class CaveDweller implements ModInitializer {
    public static final String MODID = "cavedweller";
    private static final Logger LOG = LogUtils.getLogger();
    private static ModConfigModel config;
    private boolean myBooleanOption;
    public static final CaveDwellerConfiguration CONFIG = CaveDwellerConfiguration.createAndLoad();

    public CaveDweller() {
    }

    public static ModConfigModel getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                new net.minecraft.resources.ResourceLocation(MODID, "burnt_out_torch"),
                new com.thecaptain.cavedweller.block.BurntOutTorchBlock(
                        net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.TORCH)
                                .lightLevel(state -> state.getValue(com.thecaptain.cavedweller.block.BurntOutTorchBlock.LIGHT))
                )
        );

        net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                new net.minecraft.resources.ResourceLocation(MODID, "burnt_out_wall_torch"),
                new com.thecaptain.cavedweller.block.BurntOutWallTorchBlock(
                        net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WALL_TORCH)
                                .lightLevel(state -> state.getValue(com.thecaptain.cavedweller.block.BurntOutTorchBlock.LIGHT))
                )
        );

        com.thecaptain.cavedweller.registry.ModBlockEntities.registerBlockEntities();

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.getBurntOutTorch(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.getBurntOutWallTorch(), RenderType.cutout());


        new ModItems();
        GeckoLib.initialize();
        ModSounds.registerSounds();
        ModEntityTypes.register();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
            CONFIG.save();
        }
    }
}
