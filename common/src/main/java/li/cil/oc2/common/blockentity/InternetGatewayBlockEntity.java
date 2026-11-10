/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyHandler;
import li.cil.oc2.common.inet.GatewayConnection;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.inet.InternetManager;
import li.cil.oc2.common.inet.vxlan.VXLANManager;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.InternetGatewayConfigurationMessage;
import li.cil.oc2.common.network.message.InternetGatewayStateMessage;
import li.cil.oc2.common.util.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public final class InternetGatewayBlockEntity extends ModBlockEntity implements NetworkInterface, InternetAdapter, TickableBlockEntity {
    public enum InternetMode {
        NAT,
        VXLAN
    }

    public record Settings(InternetMode mode, int gatewayAddress, String vxlanPeer, int vxlanPort, int vxlanVni) {
        private static final int DEFAULT_GATEWAY_ADDRESS = 0xC0A800FE; // 192.168.0.254

        public static Settings defaults() {
            final String peer = Config.vxlanAllowedPeers.isEmpty() ? "" : Config.vxlanAllowedPeers.getFirst();
            return new Settings(InternetMode.NAT, DEFAULT_GATEWAY_ADDRESS, peer, 4789, 0);
        }
    }

    private static final int ENERGY_SYNC_INTERVAL = 20;

    private static final String OPERATIONAL_TAG_NAME = "operational";
    private static final String SYNCED_ENERGY_TAG_NAME = "synced_energy";
    private static final String SETTINGS_TAG_NAME = "settings";
    private static final String INTERNET_MODE_TAG_NAME = "internet_mode";
    private static final String GATEWAY_ADDRESS_TAG_NAME = "gateway_address";
    private static final String VXLAN_PEER_TAG_NAME = "vxlan_peer";
    private static final String VXLAN_PORT_TAG_NAME = "vxlan_port";
    private static final String VXLAN_VNI_TAG_NAME = "vxlan_vni";
    private static final Codec<Settings> SETTINGS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.xmap(InternetMode::valueOf, InternetMode::name).fieldOf(INTERNET_MODE_TAG_NAME).forGetter(Settings::mode),
        Codec.INT.fieldOf(GATEWAY_ADDRESS_TAG_NAME).forGetter(Settings::gatewayAddress),
        Codec.STRING.fieldOf(VXLAN_PEER_TAG_NAME).forGetter(Settings::vxlanPeer),
        Codec.INT.fieldOf(VXLAN_PORT_TAG_NAME).forGetter(Settings::vxlanPort),
        Codec.INT.fieldOf(VXLAN_VNI_TAG_NAME).forGetter(Settings::vxlanVni)
    ).apply(instance, Settings::new));

    private final Deque<byte[]> toNetwork = new ArrayDeque<>();
    private final Deque<byte[]> toInternet = new ArrayDeque<>();
    private final FixedEnergyHandler energy = new FixedEnergyHandler(Config.internetGatewayEnergyStorage);

    private Settings settings = Settings.defaults();

    @Nullable
    private GatewayConnection connection;
    private boolean isOperational;
    private long syncedEnergy;

    // --------------------------------------------------------------------- //

    public InternetGatewayBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.INTERNET_GATEWAY.get(), pos, state);
        setNeedsLevelUnloadEvent();
    }

    // --------------------------------------------------------------------- //

    public boolean isOperational() {
        return isOperational;
    }

    public long getEnergyStoredClient() {
        return syncedEnergy;
    }

    public void setStateClient(final boolean isOperational, final long energy) {
        this.isOperational = isOperational;
        this.syncedEnergy = energy;
    }

    @Override
    public void serverTick() {
        final boolean newIsOperational = computeIsOperational();
        final long energy = this.energy.getEnergyStored();
        final long newSyncedEnergy = level.getGameTime() % ENERGY_SYNC_INTERVAL == 0 ? energy : syncedEnergy;
        if (newIsOperational != isOperational || newSyncedEnergy != syncedEnergy) {
            isOperational = newIsOperational;
            syncedEnergy = newSyncedEnergy;
            Network.sendToClientsTrackingBlockEntity(new InternetGatewayStateMessage(this, isOperational, syncedEnergy), this);
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public void setConfigurationClient(final Settings settings) {
        this.settings = settings;
    }

    public void setConfiguration(final Settings settings) {
        if (!settings.equals(this.settings)) {
            this.settings = settings;

            setChanged();
            stop();
            load();

            Network.sendToClientsTrackingBlockEntity(
                new InternetGatewayConfigurationMessage.ToClient(this, settings),
                this
            );
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

    private void load() {
        switch (settings.mode()) {
            case VXLAN -> connection = VXLANManager.getInstance()
                .flatMap(manager -> manager.connect(this, settings))
                .orElse(null);
            case NAT -> connection = InternetManager.getInstance()
                .map(manager -> manager.connect(this, describeOrigin(), settings.gatewayAddress()))
                .orElse(null);
        }
    }

    @Override
    protected void loadServer() {
        super.loadServer();
        load();
    }

    private void stop() {
        final GatewayConnection connection = this.connection;
        if (connection != null) {
            connection.stop();
            this.connection = null;
        }
        toNetwork.clear();
        toInternet.clear();
    }

    @Override
    protected void unloadServer(final boolean isDestroyed) {
        super.unloadServer(isDestroyed);
        stop();
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
        tag.putLong(SYNCED_ENERGY_TAG_NAME, syncedEnergy);
        tag.store(SETTINGS_TAG_NAME, SETTINGS_CODEC, settings);
        return tag;
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        energy.serialize(output.child(Constants.ENERGY_TAG_NAME));
        output.store(SETTINGS_TAG_NAME, SETTINGS_CODEC, settings);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        energy.deserialize(input.childOrEmpty(Constants.ENERGY_TAG_NAME));
        isOperational = input.getBooleanOr(OPERATIONAL_TAG_NAME, false);
        syncedEnergy = input.getLongOr(SYNCED_ENERGY_TAG_NAME, 0);
        settings = input.read(SETTINGS_TAG_NAME, SETTINGS_CODEC)
            .map(value -> Config.isVxlanEnabled() ? value : new Settings(InternetMode.NAT, value.gatewayAddress(), value.vxlanPeer(), value.vxlanPort(), value.vxlanVni()))
            .orElseGet(Settings::defaults);
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
        final String dimension = level != null ? level.dimension().identifier().toString() : "?";
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
        if (!energy.consume(Config.internetGatewayEnergyPerPacket)) {
            return false;
        }

        final Level level = getLevel();
        if (level != null) {
            ChunkUtils.setLazyUnsaved(level, getBlockPos());
        }
        return true;
    }
}
