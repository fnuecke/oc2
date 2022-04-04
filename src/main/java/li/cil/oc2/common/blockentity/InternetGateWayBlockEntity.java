package li.cil.oc2.common.blockentity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.inet.InternetManagerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InternetGateWayBlockEntity extends ModBlockEntity implements NetworkInterface, InternetAdapter {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int QUEUE_MAX = 64;
    private final Deque<byte[]> inboundQueue;
    private final Deque<byte[]> outboundQueue;

    private InternetConnection internetConnection;

    private static final String STATE_TAG = "InternetAdapter";
    private Tag internetState;

    protected InternetGateWayBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.INTERNET_GATEWAY.get(), pos, state);
        inboundQueue = new ArrayDeque<>();
        outboundQueue = new ArrayDeque<>();
        internetState = EndTag.INSTANCE;
        setNeedsLevelUnloadEvent();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(STATE_TAG)) {
            internetState = tag.get(STATE_TAG);
        } else {
            internetState = EndTag.INSTANCE;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (internetConnection != null) {
            internetConnection.saveAdapterState().ifPresent(adapterState -> tag.put(STATE_TAG, adapterState));
        }
        LOGGER.info("State saved");
    }

    @Override
    protected void loadServer() {
        InternetManagerImpl.getInstance().ifPresent(internetManager -> internetConnection = internetManager.connect(this, internetState));
        if (internetConnection != null) {
            LOGGER.info("Connected to the internet");
        } else {
            LOGGER.info("Not connected to the internet");
        }
    }

    protected void unloadServer(final boolean isRemove) {
        if (internetConnection != null) {
            internetConnection.stop();
            LOGGER.info("Connection stopped");
        }
    }

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.networkInterface(), this);
    }

    @Override
    public byte[] receiveEthernetFrame() {
        return outboundQueue.pollFirst();
    }

    @Override
    public void sendEthernetFrame(byte[] frame) {
        LOGGER.info("Got inbound packet");
        if (inboundQueue.size() < QUEUE_MAX) {
            inboundQueue.addLast(frame);
        }
    }

    @Override
    public byte[] readEthernetFrame() {
        return inboundQueue.pollFirst();
    }

    @Override
    public void writeEthernetFrame(NetworkInterface source, byte[] frame, int timeToLive) {
        LOGGER.info("Got outbound packet");
        if (outboundQueue.size() < QUEUE_MAX) {
            outboundQueue.addLast(frame);
        }
    }

}
