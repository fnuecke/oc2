/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.inet.InternetManager;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.InternetGatewayStateMessage;
import li.cil.oc2.common.util.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public final class InternetGatewayBlockEntity extends ModBlockEntity implements NetworkInterface, InternetAdapter, TickableBlockEntity {
    private static final String OPERATIONAL_TAG_NAME = "operational";

    private final Deque<byte[]> toNetwork = new ArrayDeque<>();
    private final Deque<byte[]> toInternet = new ArrayDeque<>();
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.internetGatewayEnergyStorage);

    @Nullable
    private InternetConnection connection;
    private boolean isOperational;

    // --------------------------------------------------------------------- //

    public InternetGatewayBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.INTERNET_GATEWAY.get(), pos, state);
        setNeedsLevelUnloadEvent();
    }

    // --------------------------------------------------------------------- //

    public boolean isOperational() {
        return isOperational;
    }

    public void setOperationalClient(final boolean value) {
        isOperational = value;
    }

    @Override
    public void serverTick() {
        final boolean value = computeIsOperational();
        if (value != isOperational) {
            isOperational = value;
            Network.sendToClientsTrackingBlockEntity(new InternetGatewayStateMessage(this, isOperational), this);
        }
    }

    // --------------------------------------------------------------------- //
    // NetworkInterface

    @Nullable
    @Override
    public byte[] readEthernetFrame() {
        return toNetwork.pollFirst();
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        if (timeToLive <= 0) {
            return;
        }
        if (connection == null) {
            return;
        }
        enqueue(toInternet, frame);
    }

    // --------------------------------------------------------------------- //
    // InternetAdapter

    @Nullable
    @Override
    public byte[] readInternetFrame() {
        return toInternet.pollFirst();
    }

    @Override
    public void writeInternetFrame(final byte[] frame) {
        enqueue(toNetwork, frame);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void loadServer() {
        super.loadServer();
        InternetManager.getInstance().ifPresent(manager -> connection = manager.connect(this, describeOrigin()));
    }

    @Override
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);
        final InternetConnection connection = this.connection;
        if (connection != null) {
            connection.stop();
            this.connection = null;
        }
        toNetwork.clear();
        toInternet.clear();
    }

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.NETWORK_INTERFACE, this);
        collector.offer(Capabilities.ENERGY_STORAGE, energy);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        tag.putBoolean(OPERATIONAL_TAG_NAME, isOperational);

        return tag;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        energy.deserializeNBT(tag.getCompound(Constants.ENERGY_TAG_NAME));
        isOperational = tag.getBoolean(OPERATIONAL_TAG_NAME);
    }

    // --------------------------------------------------------------------- //

    private void enqueue(final Deque<byte[]> queue, final byte[] frame) {
        if (frame.length > InternetConnection.MAX_FRAME_SIZE) {
            return;
        }
        if (queue.size() >= InternetConnection.FRAME_QUEUE_SIZE) {
            return;
        }
        if (!tryConsumeEnergy()) {
            return;
        }
        queue.addLast(frame);
    }

    private String describeOrigin() {
        final Level level = getLevel();
        final String dimension = level != null ? level.dimension().location().toString() : "?";
        final BlockPos pos = getBlockPos();
        return dimension + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private boolean computeIsOperational() {
        if (connection == null) {
            // Internet access is switched off server-side, or the stack could not start. The block
            // is inert either way, and showing it as running would be a lie.
            return false;
        }
        if (!Config.internetGatewaysUseEnergy()) {
            return true;
        }
        final long stored = energy.getEnergyStored();
        if (stored >= Config.internetGatewayEnergyPerPacket) {
            return true;
        }
        return stored > 0 && isOperational;
    }

    private boolean tryConsumeEnergy() {
        if (!Config.internetGatewaysUseEnergy()) {
            return true;
        }
        if (energy.extractEnergy(Config.internetGatewayEnergyPerPacket, true)
            < Config.internetGatewayEnergyPerPacket) {
            return false;
        }
        energy.extractEnergy(Config.internetGatewayEnergyPerPacket, false);

        final Level level = getLevel();
        if (level != null) {
            ChunkUtils.setLazyUnsaved(level, getBlockPos());
        }
        return true;
    }
}
