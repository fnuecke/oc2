/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.util.BlockEntityUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;

/**
 * Convenience interface for enabling side-specific ticking of {@link BlockEntity}s using
 * {@link BlockEntityTicker}s. Provides factory methods for all variants, working with
 * any block entity implementing this interface.
 */
public interface TickableBlockEntity {
    /**
     * Called on the client when this {@link BlockEntity} should tick.
     * <p>
     * Only called when registered using {@link #createTicker(Level, BlockEntityType, BlockEntityType)} or
     * {@link #createClientTicker(Level, BlockEntityType, BlockEntityType)}.
     */
    default void clientTick() {
    }

    /**
     * Called on the server when this {@link BlockEntity} should tick.
     * <p>
     * Only called when registered using {@link #createTicker(Level, BlockEntityType, BlockEntityType)} or
     * {@link #createServerTicker(Level, BlockEntityType, BlockEntityType)}.
     */
    default void serverTick() {
    }

    /**
     * Creates a ticker that will only run on the client.
     */
    @Nullable
    static <THave extends BlockEntity, TWant extends BlockEntity & TickableBlockEntity> BlockEntityTicker<THave> createClientTicker(final Level level, final BlockEntityType<THave> haveType, final BlockEntityType<TWant> wantType) {
        if (level.isClientSide()) {
            return BlockEntityUtils.createTicker(haveType, wantType, (ignoredLevel, blockPos, state, blockEntity) -> blockEntity.clientTick());
        } else {
            return null;
        }
    }

    /**
     * Creates a ticker that will only run on the server.
     */
    @Nullable
    static <THave extends BlockEntity, TWant extends BlockEntity & TickableBlockEntity> BlockEntityTicker<THave> createServerTicker(final Level level, final BlockEntityType<THave> haveType, final BlockEntityType<TWant> wantType) {
        if (level.isClientSide()) {
            return null;
        } else {
            return BlockEntityUtils.createTicker(haveType, wantType, (ignoredLevel, blockPos, state, blockEntity) -> blockEntity.serverTick());
        }
    }

    /**
     * Creates a ticker for either the client and the server.
     */
    @Nullable
    static <THave extends BlockEntity, TWant extends BlockEntity & TickableBlockEntity> BlockEntityTicker<THave> createTicker(final Level level, final BlockEntityType<THave> haveType, final BlockEntityType<TWant> wantType) {
        if (level.isClientSide()) {
            return BlockEntityUtils.createTicker(haveType, wantType, (ignoredLevel, blockPos, state, blockEntity) -> blockEntity.clientTick());
        } else {
            return BlockEntityUtils.createTicker(haveType, wantType, (ignoredLevel, blockPos, state, blockEntity) -> blockEntity.serverTick());
        }
    }
}
