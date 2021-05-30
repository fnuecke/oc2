package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Optional;

import javax.annotation.Nullable;

public final class NetworkHubBlockEntity extends AbstractBlockEntity implements NetworkInterface {
    private static final int TTL_COST = 1;

    ///////////////////////////////////////////////////////////////////

    private final NetworkInterface[] adjacentInterfaces = new NetworkInterface[Constants.BLOCK_FACE_COUNT];
    private boolean areAdjacentInterfacesDirty = true;

    ///////////////////////////////////////////////////////////////////

    public NetworkHubBlockEntity() {
        super(TileEntities.NETWORK_HUB_TILE_ENTITY.get());
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

        for (int i = 0; i < adjacentInterfaces.length; i++) {
            if (adjacentInterfaces[i] != null) {
                adjacentInterfaces[i].writeEthernetFrame(this, frame, timeToLive - TTL_COST);
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

        final World world = getLevel();
        if (world == null || world.isClientSide) {
            return;
        }

        final BlockPos pos = getBlockPos();
        for (final Direction side : Constants.DIRECTIONS) {
            adjacentInterfaces[side.get3DDataValue()] = null;

            final BlockEntity neighborBlockEntity = world.getBlockEntity(pos.relative(side));
            if (neighborBlockEntity != null) {
                final Optional<NetworkInterface> capability = neighborBlockEntity.getCapability(Capabilities.NETWORK_INTERFACE, side.getOpposite());
                capability.ifPresent(adjacentInterface -> {
                    adjacentInterfaces[side.get3DDataValue()] = adjacentInterface;
                    capability.addListener(unused -> handleNeighborChanged());
                });
            }
        }
    }
}
