package com.thecaptain.cavedweller;

import com.thecaptain.cavedweller.registry.ModEntityTypes;
import com.thecaptain.cavedweller.registry.ModItems;
import com.thecaptain.cavedweller.registry.ModSounds;
import com.thecaptain.cavedweller.util.config.CaveDwellerConfiguration;
import com.thecaptain.cavedweller.util.config.ModConfigModel;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

public class CaveDweller implements ModInitializer {
    public static final String MODID = "cave_dweller";
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
        new ModItems();
        GeckoLib.initialize();
        ModSounds.registerSounds();
        ModEntityTypes.register();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()) {
            CONFIG.save();
        }
    }
}
