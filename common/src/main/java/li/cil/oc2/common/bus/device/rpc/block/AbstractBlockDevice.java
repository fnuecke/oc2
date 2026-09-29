/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.util.BlockLocation;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractBlockDevice extends IdentityProxy<BlockLocation> {
    protected AbstractBlockDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    protected ServerLevel getServerLevel() {
        if (identity.tryGetLevel().orElse(null) instanceof final ServerLevel level) {
            return level;
        }

        throw new IllegalStateException("level is not loaded");
    }

    protected BlockState getBlockState(final Block block) {
        final BlockState blockState = getServerLevel().getBlockState(identity.blockPos());
        if (!blockState.is(block)) {
            throw new IllegalStateException("block has changed");
        }

        return blockState;
    }

    protected <T extends BlockEntity> T getBlockEntity(final Class<T> type) {
        final BlockEntity blockEntity = getServerLevel().getBlockEntity(identity.blockPos());
        if (type.isInstance(blockEntity)) {
            return type.cast(blockEntity);
        }

        throw new IllegalStateException("block has changed");
    }

    protected void setBlockState(final BlockState blockState) {
        checkPermission();
        getServerLevel().setBlock(identity.blockPos(), blockState, Block.UPDATE_ALL);
    }

    protected void checkPermission() {
        final ServerLevel level = getServerLevel();
        if (!level.mayInteract(FakePlayerUtils.getFakePlayer(level), identity.blockPos())) {
            throw new IllegalStateException("not allowed");
        }
    }
}
