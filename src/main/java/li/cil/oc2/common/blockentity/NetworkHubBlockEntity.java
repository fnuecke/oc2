package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.LazyOptionalUtils;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class NetworkHubBlockEntity extends ModBlockEntity implements NetworkInterface {
    private static final int TTL_COST = 1;

    ///////////////////////////////////////////////////////////////////

    private final NetworkInterface[] adjacentBlockInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private boolean haveAdjacentBlocksChanged = true;

    ///////////////////////////////////////////////////////////////////

    public NetworkHubBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.NETWORK_HUB.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    public void handleNeighborChanged() {
        haveAdjacentBlocksChanged = true;
    }

    @Override
    public byte[] readEthernetFrame() {
        return null;
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        getAdjacentInterfaces().forEach(adjacentInterface -> {
            if (adjacentInterface != source) {
                adjacentInterface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.NETWORK_INTERFACE, this);
    }

    ///////////////////////////////////////////////////////////////////

    private Stream<NetworkInterface> getAdjacentInterfaces() {
        validateAdjacentBlocks();
        return Arrays.stream(adjacentBlockInterfaces).filter(Objects::nonNull);
    }

    private void validateAdjacentBlocks() {
        if (isRemoved() || !haveAdjacentBlocksChanged) {
            return;
        }

        for (final Direction side : Constants.DIRECTIONS) {
            adjacentBlockInterfaces[side.get3DDataValue()] = null;
        }

        haveAdjacentBlocksChanged = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            final BlockEntity neighborBlockEntity = LevelUtils.getBlockEntityIfChunkExists(level, pos.relative(side));
            if (neighborBlockEntity != null) {
                final LazyOptional<NetworkInterface> optional = neighborBlockEntity.getCapability(Capabilities.NETWORK_INTERFACE, side.getOpposite());
                optional.ifPresent(adjacentInterface -> {
                    adjacentBlockInterfaces[side.get3DDataValue()] = adjacentInterface;
                    LazyOptionalUtils.addWeakListener(optional, this, (hub, unused) -> hub.handleNeighborChanged());
                });
            }
        }
    }
}
