/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.AbstractMessage;
import li.cil.oc2.common.network.message.TerminalConfigurationMessage;
import li.cil.oc2.common.network.message.TerminalOutputMessage;
import li.cil.oc2.common.network.message.TerminalStateMessage;
import li.cil.oc2.common.serial.BufferedSerialDevice;
import li.cil.oc2.common.serial.SerialEndpoint;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.TickUtils;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

public final class TerminalBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String IS_CONNECTED_TAG_NAME = "isConnected";

    public static final long CLIENT_KEEPALIVE_EVERY_MILLIS = 1000;
    private static final long CLIENT_EXPIRES_AFTER_MILLIS = 2000;

    private static final int RECIPIENT_REFRESH_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(1));
    private static final double VIEW_DISTANCE = 8;

    private static final byte FRAME_ERROR_CHAR = '~';
    private static final long FRAME_ERROR_DISPLAY_MILLIS = 100;
    private static final int CONNECTED_TIMEOUT_TICKS = 10;

    // --------------------------------------------------------------------- //

    private final Terminal terminal = new Terminal();
    private final SerialEndpoint endpoint = new SerialEndpoint(this::gameTime);
    private final Map<Player, Long> users = new WeakHashMap<>();
    private List<ServerPlayer> recipients = List.of();
    private int recipientRefreshCountdown;
    private boolean isConnected;
    private long lastFrameErrorAt;

    // --------------------------------------------------------------------- //

    public TerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.TERMINAL.get(), pos, state);
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void handleUsedBy(final Player player) {
        if (users.put(player, System.currentTimeMillis()) == null) {
            recipientRefreshCountdown = 0; // someone just opened the screen, do not make them wait
        }
    }

    public int getAddress() {
        return endpoint.getAddress();
    }

    public int getBaudRate() {
        return endpoint.getBaudRate();
    }

    public void setConfiguration(final int address, final int baudRate) {
        if (!endpoint.setConfiguration(address, baudRate)) {
            return;
        }

        setChanged();
        sendToRecipients(new TerminalConfigurationMessage.ToClient(this, endpoint.getAddress(), endpoint.getBaudRate()));
    }

    public void setConfigurationClient(final int address, final int baudRate) {
        endpoint.setConfiguration(address, baudRate);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnectedClient(final boolean value) {
        isConnected = value;
    }

    public boolean hasRecentFrameError() {
        return System.currentTimeMillis() - lastFrameErrorAt < FRAME_ERROR_DISPLAY_MILLIS;
    }

    public void handleFrameErrorClient() {
        // Intentional flicker instead of permanent on if we have consistent errors.
        final long now = System.currentTimeMillis();
        if (now - lastFrameErrorAt > FRAME_ERROR_DISPLAY_MILLIS) {
            lastFrameErrorAt = now;
        }
    }

    @Override
    public void serverTick() {
        if (--recipientRefreshCountdown <= 0) {
            recipientRefreshCountdown = RECIPIENT_REFRESH_INTERVAL;
            updateRecipients();
        }

        receive();
        transmit();

        updateConnected();
    }

    // --------------------------------------------------------------------- //

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);

        synchronized (terminal) {
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }
        endpoint.saveConfiguration(tag);
        tag.putBoolean(IS_CONNECTED_TAG_NAME, isConnected);

        return tag;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        synchronized (terminal) {
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }
        endpoint.save(tag);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        endpoint.load(tag);
        isConnected = tag.getBoolean(IS_CONNECTED_TAG_NAME);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (direction == getBlockState().getValue(OrientableBlock.FACING).getOpposite()) {
            collector.offer(Capabilities.NETWORK_INTERFACE, endpoint.getNetworkInterface());
        }
    }

    @Override
    protected void loadClient() {
        super.loadClient();

        terminal.setDisplayOnly(true);
    }

    // --------------------------------------------------------------------- //

    private void updateConnected() {
        final boolean isConnected = endpoint.getLastReadTick() >= level.getGameTime() - CONNECTED_TIMEOUT_TICKS;
        if (isConnected != this.isConnected) {
            this.isConnected = isConnected;
            Network.sendToClientsTrackingBlockEntity(new TerminalStateMessage(this, isConnected), this);
        }
    }

    private void receive() {
        byte[] output = new byte[0];
        int count = 0;
        boolean hasFrameError = false;

        int value;
        while ((value = endpoint.receive()) >= 0) {
            if (count == output.length) {
                output = Arrays.copyOf(output, Math.max(16, output.length * 2));
            }
            if ((value & BufferedSerialDevice.FRAME_ERROR_FLAG) != 0) {
                output[count++] = FRAME_ERROR_CHAR;
                hasFrameError = true;
            } else {
                output[count++] = (byte) value;
            }
        }

        if (count == 0) {
            return;
        }

        final ByteBuffer received = ByteBuffer.wrap(Arrays.copyOf(output, count));
        terminal.putOutput(received.duplicate());
        setChanged();

        sendToRecipients(new TerminalOutputMessage(this, received, hasFrameError));
    }

    private void transmit() {
        int value;
        while (endpoint.canSend() && (value = terminal.readInput()) >= 0) {
            endpoint.send((byte) value);
        }
    }

    private long gameTime() {
        return level != null ? level.getGameTime() : 0;
    }

    private Iterable<Player> getTerminalUsers() {
        final long now = System.currentTimeMillis();
        users.entrySet().removeIf(entry -> now - entry.getValue() > CLIENT_EXPIRES_AFTER_MILLIS);

        return new ArrayList<>(users.keySet());
    }

    private void updateRecipients() {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return;
        }

        final AABB bounds = AABB.ofSize(Vec3.atCenterOf(getBlockPos()),
            VIEW_DISTANCE * 2, VIEW_DISTANCE * 2, VIEW_DISTANCE * 2);
        final Set<ServerPlayer> players = new LinkedHashSet<>(serverLevel.getEntitiesOfClass(ServerPlayer.class, bounds));

        for (final Player user : getTerminalUsers()) {
            if (user instanceof final ServerPlayer serverPlayer) {
                players.add(serverPlayer);
            }
        }

        final List<ServerPlayer> previous = this.recipients;
        this.recipients = List.copyOf(players);

        for (final ServerPlayer recipient : this.recipients) {
            if (!previous.contains(recipient)) {
                recipient.connection.send(ClientboundBlockEntityDataPacket.create(this));
            }
        }
    }

    private void sendToRecipients(final AbstractMessage message) {
        for (final ServerPlayer recipient : recipients) {
            Network.sendToClient(message, recipient);
        }
    }
}
