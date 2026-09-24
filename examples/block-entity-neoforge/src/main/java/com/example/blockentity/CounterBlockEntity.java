package com.example.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CounterBlockEntity extends BlockEntity {
    private int count;

    public CounterBlockEntity(final BlockPos pos, final BlockState state) {
        super(ExampleMod.COUNTER_BLOCK_ENTITY.get(), pos, state);
    }

    public int getCount() {
        return count;
    }

    public void increment() {
        count++;
        setChanged();
    }

    public void reset() {
        count = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("count", count);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        count = tag.getInt("count");
    }
}
