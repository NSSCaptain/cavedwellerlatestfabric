package com.gargin.cavenoise;

import com.gargin.cavenoise.registry.ModEntityTypes;
import com.gargin.cavenoise.registry.ModItems;
import com.gargin.cavenoise.registry.ModSounds;
import com.gargin.cavenoise.util.config.CaveDwellerConfiguration;
import com.gargin.cavenoise.util.config.ModConfigModel;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

public class CaveNoise implements ModInitializer {
    public static final String MODID = "cavenoise";
    private static final Logger LOG = LogUtils.getLogger();
    private static ModConfigModel config;
    private boolean myBooleanOption;
    public static final CaveDwellerConfiguration CONFIG = CaveDwellerConfiguration.createAndLoad();

    public CaveNoise() {
    }

    public static ModConfigModel getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        new ModItems();
        GeckoLib.initialize();
        ModSounds.registerSounds();
        ModEntityTypes.register();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
            CONFIG.save();
        }
    }
}
