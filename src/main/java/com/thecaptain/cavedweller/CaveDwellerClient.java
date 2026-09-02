package com.thecaptain.cavedweller;

import com.thecaptain.cavedweller.registry.ModEntityTypes;
import com.thecaptain.cavedweller.client.CaveDwellerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class CaveDwellerClient implements ClientModInitializer {
    public CaveDwellerClient() {
    }

    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntityTypes.CAVEDWELLER, CaveDwellerRenderer::new);
    }
}
