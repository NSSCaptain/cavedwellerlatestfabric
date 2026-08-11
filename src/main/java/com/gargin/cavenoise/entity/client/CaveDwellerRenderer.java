package com.gargin.cavenoise.entity.client;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity> {

    public CaveDwellerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CaveDwellerModel());
        this.shadowRadius = 0.3F;
        this.addRenderLayer(new CaveDwellerEyesLayer(this));
        this.addRenderLayer(new CaveDwellerGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CaveDwellerEntity instance) {
        return new ResourceLocation("cavenoise", "textures/entity/cave_dweller_texture.png");
    }

    @Override
    public void render(CaveDwellerEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isBaby()) {
            poseStack.scale(0.1F, 0.1F, 0.1F);
        } else {
            poseStack.scale(1.3F, 1.3F, 1.3F);
        }

        java.util.Optional<software.bernie.geckolib.cache.object.GeoBone> jawBoneOptional = this.getGeoModel().getBone("jaw");
        if (jawBoneOptional.isPresent()) {
            software.bernie.geckolib.cache.object.GeoBone jawBone = jawBoneOptional.get();
            float currentTranslation = Mth.lerp(partialTick, entity.prevJawTranslation, entity.getJawTranslation());
            jawBone.setPosY(currentTranslation);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {

        float usedAlpha;

        if (isReRender) {
            usedAlpha = alpha;
        } else if (animatable.deathTime > 0 || animatable.isPlayingDeathAnimation()) {
            // 1. Calculate continuous current tick time using partial ticks for smooth rendering
            float currentTicks = (float) animatable.deathAnimationTicks + partialTick;

            // 2. Determine how far into the death animation the mob is (0.0 = start, 1.0 = end)
            // (Assumes you have a public field or getter for DEATH_ANIMATION_LENGTH in your entity)
            float progress = currentTicks / (float) animatable.DEATH_ANIMATION_LENGTH;

            // 3. Clamp progress between 0.0 and 1.0 so alpha calculations don't break
            progress = Mth.clamp(progress, 0.0F, 1.0F);

            // 4. Linearly interpolate alpha from 0.95F down to 0.0F based on progress
            usedAlpha = Mth.lerp(progress, 0.95F, 0.0F);
        } else {
            // Default 5% transparent state when alive
            usedAlpha = 0.95F;
        }

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, usedAlpha);
    }

    @Override
    public RenderType getRenderType(CaveDwellerEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}