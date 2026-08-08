package com.gargin.cavenoise;

import com.gargin.cavenoise.entity.ModEntityTypes;
import com.gargin.cavenoise.entity.client.CaveDwellerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CaveNoiseClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntityTypes.CAVE_DWELLER, CaveDwellerRenderer::new);
    }
}
