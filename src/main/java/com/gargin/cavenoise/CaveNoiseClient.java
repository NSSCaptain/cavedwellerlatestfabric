package com.gargin.cavenoise;

import com.gargin.cavenoise.registry.ModEntityTypes;
import com.gargin.cavenoise.client.CaveDwellerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CaveNoiseClient implements ClientModInitializer {
    public CaveNoiseClient() {
    }

    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntityTypes.CAVEDWELLER, CaveDwellerRenderer::new);
    }
}
