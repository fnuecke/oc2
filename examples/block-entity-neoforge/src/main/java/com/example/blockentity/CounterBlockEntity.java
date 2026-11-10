package com.example.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("count", count);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        count = input.getIntOr("count", 0);
    }
}
