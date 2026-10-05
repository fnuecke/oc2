/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Arrays;

public final class NetworkHubBlockEntity extends ModBlockEntity implements NetworkInterface {
    private static final int TTL_COST = 1;

    private int frameCount;
    private long lastGameTime;

    // --------------------------------------------------------------------- //

    private final NetworkInterface[] adjacentBlockInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private final Capabilities.InvalidationHandle[] adjacentBlockHandles = new Capabilities.InvalidationHandle[Constants.BLOCK_FACE_COUNT];
    private boolean haveAdjacentBlocksChanged = true;

    // --------------------------------------------------------------------- //

    public NetworkHubBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_HUB.get(), pos, state);
    }

    // --------------------------------------------------------------------- //

    public void handleNeighborChanged() {
        haveAdjacentBlocksChanged = true;
    }

    @Override
    @Nullable
    public byte[] readEthernetFrame() {
        return null;
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        if (level == null) {
            return;
        }

        // Give a cap on top of the TLL, just in case trolls intentionally build
        // loops that exponentially multiply ethernet frames after people crank up
        // the default TTL.
        final long gameTime = level.getGameTime();
        if (gameTime > lastGameTime) {
            lastGameTime = gameTime;
            frameCount = 1;
        } else if (frameCount > Config.hubEthernetFramesPerTick) {
            return;
        } else {
            frameCount++;
        }

        validateAdjacentBlocks();

        for (final NetworkInterface adjacentInterface : adjacentBlockInterfaces) {
            if (adjacentInterface != null && adjacentInterface != source) {
                adjacentInterface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            }
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.NETWORK_INTERFACE, this);
    }

    // --------------------------------------------------------------------- //

    private void validateAdjacentBlocks() {
        if (!isValid() || !haveAdjacentBlocksChanged) {
            return;
        }

        for (final var handle : adjacentBlockHandles) {
            if (handle != null) {
                handle.drop();
            }
        }
        Arrays.fill(adjacentBlockHandles, null);
        Arrays.fill(adjacentBlockInterfaces, null);

        haveAdjacentBlocksChanged = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            final BlockPos neighborPos = pos.relative(side);
            adjacentBlockHandles[side.get3DDataValue()] = Capabilities.listen((ServerLevel) level, neighborPos, () -> haveAdjacentBlocksChanged = true);
            if (level.isLoaded(neighborPos)) {
                adjacentBlockInterfaces[side.get3DDataValue()] = Capabilities.get(level, neighborPos, Capabilities.NETWORK_INTERFACE, side.getOpposite());
            }
        }
    }
}
