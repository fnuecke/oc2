package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public final class NetworkHubTileEntity extends AbstractTileEntity implements NetworkInterface {
    private static final int TTL_COST = 1;

    ///////////////////////////////////////////////////////////////////

    private final NetworkInterface[] adjacentInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private boolean areAdjacentInterfacesDirty = true;

    ///////////////////////////////////////////////////////////////////

    public NetworkHubTileEntity(final BlockPos pos, final BlockState state) {
        super(TileEntities.NETWORK_HUB_TILE_ENTITY.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    public void handleNeighborChanged() {
        areAdjacentInterfacesDirty = true;
    }

    @Override
    public byte[] readEthernetFrame() {
        return null;
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        validateAdjacentInterfaces();

        for (final NetworkInterface adjacentInterface : adjacentInterfaces) {
            if (adjacentInterface != null && adjacentInterface != source) {
                adjacentInterface.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.NETWORK_INTERFACE, this);
    }

    ///////////////////////////////////////////////////////////////////

    private void validateAdjacentInterfaces() {
        if (!areAdjacentInterfacesDirty || isRemoved()) {
            return;
        }

        areAdjacentInterfacesDirty = false;

        if (level == null || level.isClientSide()) {
            return;
        }

        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            adjacentInterfaces[side.get3DDataValue()] = null;

            final BlockEntity neighborTileEntity = level.getBlockEntity(pos.relative(side));
            if (neighborTileEntity != null) {
                final LazyOptional<NetworkInterface> capability = neighborTileEntity.getCapability(Capabilities.NETWORK_INTERFACE, side.getOpposite());
                capability.ifPresent(adjacentInterface -> {
                    adjacentInterfaces[side.get3DDataValue()] = adjacentInterface;
                    capability.addListener(unused -> handleNeighborChanged());
                });
            }
        }
    }
}
