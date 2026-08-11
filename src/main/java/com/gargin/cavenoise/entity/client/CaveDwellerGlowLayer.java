package com.gargin.cavenoise.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

public class CaveDwellerGlowLayer extends GeoRenderLayer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("cavenoise", "textures/entity/cave_dweller_glow_texture.png");

    public CaveDwellerGlowLayer(GeoRenderer entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.entityTranslucent(TEXTURE);
        GeoBone headBone = bakedModel.getBone("head").orElse(null);
        if (headBone != null) {
            poseStack.pushPose();

            RenderUtils.translateMatrixToBone(poseStack, headBone);

            // Eye glow offset from head
            // BUG: Glow seems to be additionally randomly offset based on spawn conditions?
            poseStack.translate(0.01F, 0.0F, 0.0F);

            // Eye glow transparency
            float glowAlpha = 0.15F;

            // Separate the glow's transparency from the main model
            VertexConsumer isolatedBuffer = bufferSource.getBuffer(glowRenderType);

            this.getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    glowRenderType,
                    isolatedBuffer,
                    partialTick,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, glowAlpha
            );

            poseStack.popPose();
        }
    }
}