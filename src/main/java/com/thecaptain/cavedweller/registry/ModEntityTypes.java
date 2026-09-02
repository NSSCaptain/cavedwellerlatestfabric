package com.thecaptain.cavedweller.registry;

import com.thecaptain.cavedweller.entities.CaveDwellerEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ModEntityTypes {
    public static final EntityType<? extends CaveDwellerEntity> CAVEDWELLER = register(new ResourceLocation("cave_dweller", "cave_dweller").getPath(), CaveDwellerEntity::new, 0.4F, 1.9F);

    public ModEntityTypes() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation("cave_dweller", name), FabricEntityTypeBuilder.create(MobCategory.MONSTER, factory).dimensions(EntityDimensions.scalable(width, height)).trackedUpdateRate(1).build());
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(CAVEDWELLER, CaveDwellerEntity.getAttributeBuilder());

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
                if (spawnEgg.getType(null) == CAVEDWELLER) {

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
