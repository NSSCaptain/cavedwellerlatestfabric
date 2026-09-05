package com.thecaptain.cavedweller.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BurntOutWallTorchBlock extends WallTorchBlock {
    public BurntOutWallTorchBlock(Properties properties) {
        super(properties, ParticleTypes.SMOKE);
        this.registerDefaultState(this.stateDefinition.any().setValue(BurntOutTorchBlock.LIGHT, 14));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && state.getValue(BurntOutTorchBlock.LIGHT) == 14) {
            level.scheduleTick(pos, this, 3);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int currentLight = state.getValue(BurntOutTorchBlock.LIGHT);
        if (currentLight > 0) {
            level.setBlock(pos, state.setValue(BurntOutTorchBlock.LIGHT, currentLight - 1), 3);
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int currentLight = state.getValue(BurntOutTorchBlock.LIGHT);
        if (currentLight == 14) {
            super.animateTick(state, level, pos, random);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(Items.TORCH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BurntOutTorchBlock.LIGHT);
    }
}