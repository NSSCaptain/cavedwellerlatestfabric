package com.thecaptain.cavedweller.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BurntOutTorchBlockEntity extends BlockEntity {
    public int currentLight = 14;
    public int tickCounter = 0;

    public BurntOutTorchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public BurntOutTorchBlockEntity(BlockPos pos, BlockState state) {
        super(com.thecaptain.cavedweller.registry.ModBlockEntities.BURNT_OUT_TORCH_ENTITY, pos, state);
    }
}
