package com.gargin.cavenoise.entity.client;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity> {

    // Makes sure the death animation plays out and the dweller is fully transparent (invisible) before falling over and "poofing"
    public static float timingOffset = 0.3F;

    @Override
    public boolean shouldShowName(CaveDwellerEntity entity) {
        return false;
    }

    private static class RenderTypeAccessor extends RenderType {
        private RenderTypeAccessor(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        }

        public static RenderType getCustomEmissiveTranslucent(ResourceLocation texture) {
            CompositeState state = CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true);

            return create("cave_dweller_glowing_translucent_layer",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,
                    true,
                    state);
        }
    }

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
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float usedAlpha;
        // Dweller is always 15% transparent
        // TODO: (except for the head)
        float defaultAlpha = 0.85F;

        if (isReRender) {
            usedAlpha = alpha;
        } else if (animatable.deathTime > 0 || animatable.isPlayingDeathAnimation()) {
            float currentTicks = (float) (animatable.deathAnimationTicks) + partialTick;
            float progress = (currentTicks / (float) animatable.DEATH_ANIMATION_LENGTH) + timingOffset;
            progress = Mth.clamp(progress, 0.0F, 1.0F);
            usedAlpha = Mth.lerp(progress, defaultAlpha, 0.0F);
        } else {
            usedAlpha = defaultAlpha;
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

    // ==========================================
    //   INNER CHILD LAYER: EYES
    // ==========================================
    private static class CaveDwellerEyesLayer extends GeoRenderLayer<CaveDwellerEntity> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("cavenoise", "textures/entity/cave_dweller_eyes_texture.png");
        private final RenderType eyesRenderType = RenderTypeAccessor.getCustomEmissiveTranslucent(TEXTURE);

        public CaveDwellerEyesLayer(GeoRenderer<CaveDwellerEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            packedLight = 15728880;
            float usedAlpha;

            if (animatable.deathTime > 0 || animatable.isPlayingDeathAnimation()) {
                float currentTicks = (float) animatable.deathAnimationTicks + partialTick;
                float progress = (currentTicks / (float) animatable.DEATH_ANIMATION_LENGTH) + (timingOffset - 4.0F);
                progress = Mth.clamp(progress, 0.0F, 1.0F);
                usedAlpha = Mth.lerp(progress, 1.0F, 0.0F);
            } else {
                usedAlpha = 1.0F;
            }

            VertexConsumer customBuffer = bufferSource.getBuffer(this.eyesRenderType);
            this.getRenderer().reRender(this.getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, this.eyesRenderType, customBuffer, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, usedAlpha);
        }
    }

    // ==========================================
    //   INNER CHILD LAYER: GLOW (HEAD OVERLAY)
    // ==========================================
    private static class CaveDwellerGlowLayer extends GeoRenderLayer<CaveDwellerEntity> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("cavenoise", "textures/entity/cave_dweller_glow_texture.png");
        private final RenderType glowRenderType = RenderTypeAccessor.getCustomEmissiveTranslucent(TEXTURE);

        public CaveDwellerGlowLayer(GeoRenderer<CaveDwellerEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (bakedModel.getBone("head").isEmpty()) return;

            float usedAlpha;

            if (animatable.deathTime > 0 || animatable.isPlayingDeathAnimation()) {
                float currentTicks = (float) animatable.deathAnimationTicks + partialTick;
                float progress = (currentTicks / (float) animatable.DEATH_ANIMATION_LENGTH) + timingOffset;
                progress = Mth.clamp(progress, 0.0F, 1.0F);
                usedAlpha = Mth.lerp(progress, 0.05F, 0.0F);
            } else {
                usedAlpha = 0.05F;
            }

            VertexConsumer customBuffer = bufferSource.getBuffer(this.glowRenderType);
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, this.glowRenderType, customBuffer, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, usedAlpha);
        }
    }
}
