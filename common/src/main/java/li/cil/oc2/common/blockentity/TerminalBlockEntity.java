/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.AbstractMessage;
import li.cil.oc2.common.network.message.TerminalConfigurationMessage;
import li.cil.oc2.common.network.message.TerminalOutputMessage;
import li.cil.oc2.common.serial.BufferedSerialDevice;
import li.cil.oc2.common.serial.SerialFrame;
import li.cil.oc2.common.serial.SerialLine;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.LevelUtils;
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

import static java.util.Objects.requireNonNull;

public final class TerminalBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    private static final String TERMINAL_TAG_NAME = "terminal";
    private static final String PORT_TAG_NAME = "port";
    private static final String LINE_TAG_NAME = "line";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String BAUD_RATE_TAG_NAME = "baudRate";

    public static final long CLIENT_KEEPALIVE_EVERY_MILLIS = 1000;
    private static final long CLIENT_EXPIRES_AFTER_MILLIS = 2000;
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final int MIN_BAUD_RATE = 300;
    private static final int MAX_BAUD_RATE = 115200;

    private static final int RECIPIENT_REFRESH_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(1));
    private static final double VIEW_DISTANCE = 8;

    private static final byte FRAME_ERROR_CHAR = '~';
    private static final long FRAME_ERROR_DISPLAY_MILLIS = 100;

    // --------------------------------------------------------------------- //

    private final Terminal terminal = new Terminal();
    private final BufferedSerialDevice port = new BufferedSerialDevice();
    private final NetworkInterface networkInterface = new TerminalNetworkInterface();
    private final Map<Player, Long> users = new WeakHashMap<>();
    private List<ServerPlayer> recipients = List.of();
    private int recipientRefreshCountdown;
    private long lastFrameErrorAt;

    private int address = SerialFrame.getRandomAddress();
    private int baudRate = DEFAULT_BAUD_RATE;

    @Nullable
    private SerialLine line;
    @Nullable
    private CompoundTag lineTag;

    // --------------------------------------------------------------------- //

    public TerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.TERMINAL.get(), pos, state);
        port.setBaudRate(baudRate);
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
        return address;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setConfiguration(final int address, final int baudRate) {
        final int newAddress = Math.clamp(address, 0, SerialFrame.MAX_ADDRESS);
        final int newBaudRate = Math.clamp(baudRate, MIN_BAUD_RATE, MAX_BAUD_RATE);
        if (newAddress == this.address && newBaudRate == this.baudRate) {
            return;
        }

        this.address = newAddress;
        this.baudRate = newBaudRate;

        port.setBaudRate(newBaudRate);
        line = null;
        lineTag = null;

        setChanged();
        sendToRecipients(new TerminalConfigurationMessage.ToClient(this, newAddress, newBaudRate));
    }

    public void setConfigurationClient(final int address, final int baudRate) {
        this.address = address;
        this.baudRate = baudRate;
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
        tag.putInt(ADDRESS_TAG_NAME, address);
        tag.putInt(BAUD_RATE_TAG_NAME, baudRate);

        return tag;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        synchronized (terminal) {
            tag.put(TERMINAL_TAG_NAME, NBTSerialization.serialize(terminal));
        }
        tag.put(PORT_TAG_NAME, NBTSerialization.serialize(port));
        if (line != null) {
            tag.put(LINE_TAG_NAME, NBTSerialization.serialize(line));
        }
        tag.putInt(ADDRESS_TAG_NAME, address);
        tag.putInt(BAUD_RATE_TAG_NAME, baudRate);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        NBTSerialization.deserialize(tag.getCompound(TERMINAL_TAG_NAME), terminal);
        NBTSerialization.deserialize(tag.getCompound(PORT_TAG_NAME), port);
        address = tag.getInt(ADDRESS_TAG_NAME);
        baudRate = tag.getInt(BAUD_RATE_TAG_NAME);
        lineTag = tag.contains(LINE_TAG_NAME) ? tag.getCompound(LINE_TAG_NAME) : null;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (direction == getBlockState().getValue(OrientableBlock.FACING).getOpposite()) {
            collector.offer(Capabilities.NETWORK_INTERFACE, networkInterface);
        }
    }

    @Override
    protected void loadClient() {
        super.loadClient();

        terminal.setDisplayOnly(true);
    }

    // --------------------------------------------------------------------- //

    private void receive() {
        byte[] output = new byte[0];
        int count = 0;
        boolean hasFrameError = false;

        int value;
        while ((value = port.receive()) >= 0) {
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
        while (port.canSend() && (value = terminal.readInput()) >= 0) {
            port.send((byte) value);
        }
    }

    private SerialLine getLine() {
        SerialLine result = line;
        if (result == null) {
            result = new SerialLine(port, LevelUtils.gameTimeSupplier(requireNonNull(level)), SerialFrame.macOf(address));
            if (lineTag != null) {
                NBTSerialization.deserialize(lineTag, result);
                lineTag = null;
            }
            line = result;
        }

        return result;
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

    // --------------------------------------------------------------------- //

    private final class TerminalNetworkInterface implements NetworkInterface {
        private long servedTick = Long.MIN_VALUE;

        @Nullable
        @Override
        public byte[] readEthernetFrame() {
            final SerialLine line = getLine();

            final long tick = line.currentTick();
            if (tick == servedTick) {
                return null;
            }

            servedTick = tick;
            return line.frameForTick();
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
            getLine().writeEthernetFrame(frame);
        }
    }
}
