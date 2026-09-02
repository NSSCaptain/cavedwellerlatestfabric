package com.thecaptain.cavedweller.client;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class CaveDwellerModel extends GeoModel<CaveDwellerEntity> {

    private float defaultJawY = Float.NaN;
    private float defaultThroatScaleY = Float.NaN;

    public CaveDwellerModel() {
    }

    @Override
    public ResourceLocation getModelResource(CaveDwellerEntity object) {
        return new ResourceLocation("cavedweller", "geo/cave_dweller.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CaveDwellerEntity object) {
        return new ResourceLocation("cavedweller", "textures/entity/cave_dweller_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CaveDwellerEntity animatable) {
        return new ResourceLocation("cavedweller", "animations/cave_dweller.animation.json");
    }

    @Override
    public void setCustomAnimations(CaveDwellerEntity animatable, long instanceId, AnimationState<CaveDwellerEntity> animationState) {
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        CoreGeoBone jawBone = this.getAnimationProcessor().getBone("jaw");
        CoreGeoBone throatBone = this.getAnimationProcessor().getBone("throat");
        CoreGeoBone lowerBody = this.getAnimationProcessor().getBone("lowerbody");
        if (head != null) {
            // ONLY track the player if dweller NOT playing its death animation
            if (animatable.deathTime == 0 && !animatable.isPlayingDeathAnimation()) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
                head.setRotX(entityData.headPitch() * ((float) Math.PI / 180F));
                head.setRotY(entityData.netHeadYaw() * ((float) Math.PI / 180F));
            }
        }

        if (lowerBody != null) {
            if ((Boolean) animatable.getEntityData().get(CaveDwellerEntity.CLIMBING_ACCESSOR)) {
                lowerBody.setRotX(0.0F);
                lowerBody.setRotY(0.0F);
                lowerBody.setRotZ(0.0F);
            }
        }

        /// Dynamic jaw animation
        if (jawBone != null) {
            if (Float.isNaN(defaultJawY)) {
                defaultJawY = jawBone.getPosY();
            }

            int currentTick = animatable.getJawAnimationTick();

            if (currentTick >= 0) {
                float dynamicSpeed = animatable.getJawSpeed();
                float dynamicDistance = animatable.getJawDistance();
                float dynamicHoldTime = animatable.getJawHoldTime();

                float elapsedSeconds = currentTick / 20.0F;

                float transitionDuration = 1.0F / dynamicSpeed;
                float closeStartTime = transitionDuration + dynamicHoldTime;
                float totalDuration = closeStartTime + transitionDuration;

                if (elapsedSeconds >= totalDuration) {
                    animatable.resetJawAnimation();
                    jawBone.setPosY(defaultJawY);
                } else {
                    float linearFactor = 0.0F;

                    // 1. Calculate the raw linear progression (0.0 to 1.0)
                    if (elapsedSeconds < transitionDuration) {
                        // Phase A: Opening
                        linearFactor = elapsedSeconds / transitionDuration;
                    } else if (elapsedSeconds < closeStartTime) {
                        // Phase B: Holding
                        linearFactor = 1.0F;
                    } else {
                        // Phase C: Closing
                        float closingElapsed = elapsedSeconds - closeStartTime;
                        linearFactor = 1.0F - (closingElapsed / transitionDuration);
                    }
                    float smoothedFactor = (float) (1.0 - Math.cos(linearFactor * Math.PI)) / 2.0F;
                    jawBone.setPosY(defaultJawY - (smoothedFactor * dynamicDistance));
                }
            } else {
                jawBone.setPosY(defaultJawY);
            }
        }
        // Scale the throat along with the jaw
        if (throatBone != null) {
            if (Float.isNaN(defaultThroatScaleY)) {
                defaultThroatScaleY = throatBone.getScaleY();
            }

            int currentTick = animatable.getJawAnimationTick();

            if (currentTick >= 0) {
                float dynamicSpeed = animatable.getJawSpeed();
                float dynamicDistance = animatable.getJawDistance();
                float dynamicHoldTime = animatable.getJawHoldTime();

                float elapsedSeconds = currentTick / 20.0F;

                float transitionDuration = 1.0F / dynamicSpeed;
                float closeStartTime = transitionDuration + dynamicHoldTime;
                float totalDuration = closeStartTime + transitionDuration;

                if (elapsedSeconds >= totalDuration) {
                    throatBone.setScaleY(defaultThroatScaleY);
                } else {
                    float linearFactor = 0.0F;

                    if (elapsedSeconds < transitionDuration) {
                        linearFactor = elapsedSeconds / transitionDuration;
                    } else if (elapsedSeconds < closeStartTime) {
                        linearFactor = 1.0F;
                    } else {
                        float closingElapsed = elapsedSeconds - closeStartTime;
                        linearFactor = 1.0F - (closingElapsed / transitionDuration);
                    }

                    // Smooth curve matching the jaw speed
                    float smoothedFactor = (float) (1.0 - Math.cos(linearFactor * Math.PI)) / 2.0F;

                    // Scale multiplier: Adjust float according to size of "throat" texture
                    float throatHeightInPixels = 4.0F;

                    float throatHeight = 1.0F / throatHeightInPixels;
                    float scaleIncrease = smoothedFactor * dynamicDistance * throatHeight;
                    throatBone.setScaleY(defaultThroatScaleY + scaleIncrease);
                }
            } else {
                // Return to default scale when jaw is closed
                throatBone.setScaleY(defaultThroatScaleY);
            }
        }
    }
}
