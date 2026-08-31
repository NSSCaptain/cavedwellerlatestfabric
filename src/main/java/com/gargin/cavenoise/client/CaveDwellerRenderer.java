package com.gargin.cavenoise.client;

import com.gargin.cavenoise.entities.CaveDwellerEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity> {

    // Makes sure the death animation plays out and the dweller is fully transparent (invisible) before falling over and "poofing"
    public static float timingOffset = 0.3F;
    private float currentRenderAlpha = 0.85F;
    public boolean isRenderingCustomLayer = false;

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

        float newYaw = entityYaw;

        if ((Boolean) entity.getEntityData().get(CaveDwellerEntity.CLIMBING_ACCESSOR)) {
            newYaw = (Float) entity.getEntityData().get(CaveDwellerEntity.CLIMB_ANGLE_ACCESSOR);

            if (entity.getEntityData().get(CaveDwellerEntity.CLIMB_WALL_ACCESSOR) == Direction.WEST) {
                System.out.println("Direction: WEST");
            } else if (entity.getEntityData().get(CaveDwellerEntity.CLIMB_WALL_ACCESSOR) == Direction.NORTH) {
                System.out.println("Direction: NORTH");
                poseStack.translate(0.0, 0.0, -0.2);
            } else if (entity.getEntityData().get(CaveDwellerEntity.CLIMB_WALL_ACCESSOR) == Direction.EAST) {
                System.out.println("Direction: EAST");
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
            } else if (entity.getEntityData().get(CaveDwellerEntity.CLIMB_WALL_ACCESSOR) == Direction.SOUTH) {
                System.out.println("Direction: SOUTH");
                poseStack.translate(0.0, 0.0, 0.6);
                poseStack.mulPose(Axis.XN.rotationDegrees(90));
            }
        }

        super.render(entity, newYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (this.isRenderingCustomLayer) {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        float dynamicAlpha = alpha;
        CaveDwellerEntity entity = getAnimatable();

        if (entity != null && alpha > 0.0F) {
            float masterAlpha = entity.getRenderAlpha();
            float baseTargetAlpha = bone.getName().equals("head") ? masterAlpha : (masterAlpha * 0.85F);

            if (entity.deathTime > 0 || entity.isPlayingDeathAnimation()) {
                float currentTicks = (float) (entity.deathAnimationTicks);
                float progress = (currentTicks / (float) entity.deathAnimationLength) + timingOffset;
                progress = Mth.clamp(progress, 0.0F, 1.0F);
                dynamicAlpha = Mth.lerp(progress, baseTargetAlpha, 0.0F);
            } else {
                dynamicAlpha = baseTargetAlpha;
            }
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, red, green, blue, dynamicAlpha);
    }

    @Override
    public RenderType getRenderType(CaveDwellerEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    private static class CaveDwellerEyesLayer extends GeoRenderLayer<CaveDwellerEntity> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("cavenoise", "textures/entity/cave_dweller_eyes_texture.png");
        private final RenderType eyesRenderType = RenderTypeAccessor.getCustomEmissiveTranslucent(TEXTURE);

        public CaveDwellerEyesLayer(GeoRenderer<CaveDwellerEntity> entityRendererIn) {
            super(entityRendererIn);
        }

        @Override
        public void render(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            int maxLight = 15728880;
            float usedAlpha;

            if (animatable.deathTime > 0 || animatable.isPlayingDeathAnimation()) {
                float currentTicks = (float) animatable.deathAnimationTicks + partialTick;
                float progress = (currentTicks / (float) animatable.deathAnimationLength) + timingOffset;
                progress = Mth.clamp(progress, 0.0F, 1.0F);
                usedAlpha = Mth.lerp(progress, animatable.getRenderAlpha(), 0.0F);
            } else {
                usedAlpha = animatable.getRenderAlpha();
            }

            VertexConsumer customBuffer = bufferSource.getBuffer(this.eyesRenderType);
            if (this.getRenderer() instanceof CaveDwellerRenderer renderer) {
                renderer.isRenderingCustomLayer = true;
            }

            this.getRenderer().reRender(this.getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, this.eyesRenderType, customBuffer, partialTick, maxLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, usedAlpha);

            if (this.getRenderer() instanceof CaveDwellerRenderer renderer) {
                renderer.isRenderingCustomLayer = false;
            }
        }
    }

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
                float progress = (currentTicks / (float) animatable.deathAnimationLength) + timingOffset;
                progress = Mth.clamp(progress, 0.0F, 1.0F);
                usedAlpha = Mth.lerp(progress, 0.05F * animatable.getRenderAlpha(), 0.0F);
            } else {
                usedAlpha = 0.05F * animatable.getRenderAlpha();
            }

            VertexConsumer customBuffer = bufferSource.getBuffer(this.glowRenderType);

            if (this.getRenderer() instanceof CaveDwellerRenderer renderer) {
                renderer.isRenderingCustomLayer = true;
            }

            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, this.glowRenderType, customBuffer, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, usedAlpha);

            if (this.getRenderer() instanceof CaveDwellerRenderer renderer) {
                renderer.isRenderingCustomLayer = false;
            }
        }
    }
}
