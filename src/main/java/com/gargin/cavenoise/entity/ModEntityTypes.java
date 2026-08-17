package com.gargin.cavenoise.entity;

import com.gargin.cavenoise.entity.custom.CaveDwellerEntity;
import com.gargin.cavenoise.CaveNoise;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ModEntityTypes {
    public static final EntityType<CaveDwellerEntity> CAVE_DWELLER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            new ResourceLocation(CaveNoise.MODID, "cave_dweller"),
            EntityType.Builder.of(CaveDwellerEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F)
                    .build(new ResourceLocation(CaveNoise.MODID, "cave_dweller").toString())
    );

    // Prevents spawning more than one dweller
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof CaveDwellerEntity newDweller && !world.isClientSide()) {
                long activeDwellers = world.getEntitiesOfClass(CaveDwellerEntity.class,
                        newDweller.getBoundingBox().inflate(5000.0D),
                        other -> other != newDweller && other.isAlive()
                ).size();

                if (activeDwellers > 0) {
                    Player closestPlayer = world.getNearestPlayer(newDweller, 32.0D);
                    if (closestPlayer != null) {
                        closestPlayer.sendSystemMessage(
                                Component.literal("§4Can not spawn another Cave Dweller")
                        );
                        closestPlayer.sendSystemMessage(
                                Component.literal("§4It already exists!")
                        );
                    }

                    newDweller.discard();
                }
            }
        });

        // Prevents using up a dweller spawn egg if a dweller is already present
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player.getItemInHand(hand).getItem() instanceof SpawnEggItem spawnEgg) {
                if (spawnEgg.getType(null) == CAVE_DWELLER) {

                    // Interaction fix not present on Bedrock
                    // Checks if you're just trying to interact with something first
                    // Also prevents the "Can not spawn another Cave Dweller" messages from appearing if so
                    BlockState state = world.getBlockState(hitResult.getBlockPos());
                    if (state.is(net.minecraft.tags.BlockTags.DOORS) ||
                            state.is(net.minecraft.tags.BlockTags.TRAPDOORS) ||
                            state.is(net.minecraft.tags.BlockTags.FENCE_GATES) ||
                            state.is(net.minecraft.tags.BlockTags.BUTTONS) ||
                            state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock ||
                            state.getMenuProvider(world, hitResult.getBlockPos()) != null) {

                        return InteractionResult.PASS;
                    }

                    ServerLevel serverLevel = (ServerLevel) world;

                    long activeDwellers = serverLevel.getEntitiesOfClass(CaveDwellerEntity.class,
                            new AABB(player.blockPosition()).inflate(5000.0D),
                            LivingEntity::isAlive
                    ).size();

                    if (activeDwellers > 0) {
                        player.sendSystemMessage(
                                Component.literal("§4Can not spawn another Cave Dweller")
                        );
                        player.sendSystemMessage(
                                Component.literal("§4It already exists!")
                        );

                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}