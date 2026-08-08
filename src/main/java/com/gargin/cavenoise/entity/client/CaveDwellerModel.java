package com.gargin.cavenoise.entity.client;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class CaveDwellerModel extends GeoModel<CaveDwellerEntity> {
    public CaveDwellerModel() {
    }

    @Override
    public ResourceLocation getModelResource(CaveDwellerEntity object) {
        return new ResourceLocation("cavenoise", "geo/cave_dweller.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CaveDwellerEntity object) {
        return new ResourceLocation("cavenoise", "textures/entity/cave_dweller_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CaveDwellerEntity animatable) {
        return new ResourceLocation("cavenoise", "animations/cave_dweller.animation.json");
    }

    @Override
    public void setCustomAnimations(CaveDwellerEntity animatable, long instanceId, AnimationState<CaveDwellerEntity> animationState) {
        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = (EntityModelData)animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * ((float)Math.PI / 180F));
            head.setRotY(entityData.netHeadYaw() * ((float)Math.PI / 180F));
        }
    }
}
