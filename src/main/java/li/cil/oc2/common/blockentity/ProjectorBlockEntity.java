package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.bus.device.vm.ProjectorVMDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ProjectorFramebufferMessage;
import li.cil.oc2.common.network.message.ProjectorStateMessage;
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

// TODO Only send frames to watching clients (have clients send "keepalive" packets when rendering this).
// TODO Throttle update speed by distance to closest player and max number any player watching this projector is watching in total.
public final class ProjectorBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    @FunctionalInterface
    public interface FrameConsumer {
        void processFrame(final Picture picture);
    }

    ///////////////////////////////////////////////////////////////

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int MAX_RENDER_DISTANCE = 16;
    public static final int MAX_GOOD_RENDER_DISTANCE = 12;
    public static final int MAX_WIDTH = MAX_GOOD_RENDER_DISTANCE + 1; // +1 To make it odd, so we can center.
    public static final int MAX_HEIGHT = (MAX_GOOD_RENDER_DISTANCE * ProjectorVMDevice.HEIGHT / ProjectorVMDevice.WIDTH) + 1; // + 1 To match horizontal margin.

    private static final int FRAME_EVERY_N_TICKS = 5;

    private static final String ENERGY_TAG_NAME = "energy";
    private static final String IS_PROJECTING_TAG_NAME = "projecting";

    private static final ExecutorService FRAME_WORKERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Projector Frame Encoder/Decoder");
        return thread;
    });

    ///////////////////////////////////////////////////////////////

    private final ProjectorVMDevice projectorDevice = new ProjectorVMDevice(this);
    private boolean isProjecting, hasEnergy;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.projectorEnergyStorage);

    // Video transfer.
    private final H264Encoder encoder = new H264Encoder(new CQPRateControl(12));
    @Nullable private Future<?> runningEncode; // To allow waiting for previous frame to finish.
    private final Picture picture = Picture.create(ProjectorVMDevice.WIDTH, ProjectorVMDevice.HEIGHT, ColorSpace.YUV420J);

    private boolean needsIDR = true; // Whether we need to send a keyframe next.
    private final AtomicInteger sendBudget = new AtomicInteger(); // Remaining accumulated bandwidth budget.
    private int nextFrameIn = 0; // Remaining cooldown before sending next frame.

    // Client only data.
    private final H264Decoder decoder = new H264Decoder();
    @Nullable private Future<?> runningDecode; // Current decoding operation, if any, to avoid race conditions.
    private final ByteBuffer decoderBuffer = ByteBuffer.allocateDirect(1024 * 1024); // Re-used decompression buffer.
    @Nullable private FrameConsumer frameConsumer; // Where to throw received frames.

    private AABB renderBounds; // Overall render bounds, disregarding projection surface, to allow growing if necessary.

    ///////////////////////////////////////////////////////////////

    public ProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PROJECTOR.get(), pos, state);

        encoder.setKeyInterval(100);

        updateRenderBounds();
    }

    ///////////////////////////////////////////////////////////////

    public ProjectorVMDevice getProjectorDevice() {
        return projectorDevice;
    }

    public boolean isProjecting() {
        return isProjecting;
    }

    public void setProjecting(final boolean value) {
        isProjecting = value;

        if (!isProjecting) {
            Arrays.fill(picture.getPlaneData(0), (byte) -128);
            Arrays.fill(picture.getPlaneData(1), (byte) 0);
            Arrays.fill(picture.getPlaneData(2), (byte) 0);
            needsIDR = true;
        }

        sendRunningState();
    }

    public void setFrameConsumer(@Nullable final FrameConsumer consumer) {
        if (consumer == frameConsumer) {
            return;
        }
        synchronized (picture) {
            this.frameConsumer = consumer;
            if (frameConsumer != null) {
                frameConsumer.processFrame(picture);
            }
        }
    }

    @Override
    public void serverTick() {
        if (!isProjecting()) {
            return;
        }

        if (energy.extractEnergy(Config.projectorEnergyPerTick, true) < Config.projectorEnergyPerTick) {
            if (hasEnergy) {
                hasEnergy = false;
                sendRunningState();
            }
            return;
        } else if (!hasEnergy) {
            hasEnergy = true;
            sendRunningState();
        }

        sendBudget.updateAndGet(budget -> Math.min(Config.projectorMaxBytesPerTick * 10, budget + Config.projectorMaxBytesPerTick));
        nextFrameIn = Math.max(0, nextFrameIn - 1);
        if (sendBudget.get() < 0 || nextFrameIn > 0) {
            return;
        }

        if (runningEncode != null && !runningEncode.isDone()) {
            return;
        }

        joinWorkerAndLogErrors(runningEncode);

        nextFrameIn = FRAME_EVERY_N_TICKS;

        if (level == null || !(level.getChunk(getBlockPos()) instanceof LevelChunk chunk)) {
            return;
        }

        runningEncode = FRAME_WORKERS.submit(() -> {
            final boolean hasChanges = projectorDevice.applyChanges(picture);
            if (!hasChanges && !needsIDR) {
                return;
            }

            final ByteBuffer frameData;
            if (needsIDR) {
                frameData = encoder.encodeIDRFrame(picture, ByteBuffer.allocateDirect(256 * 1024));
                needsIDR = false;
            } else {
                frameData = encoder.encodeFrame(picture, ByteBuffer.allocateDirect(256 * 1024)).data();
            }

            final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(frameData);
            deflater.finish();
            final ByteBuffer compressedFrameData = ByteBuffer.allocateDirect(1024 * 1024);
            deflater.deflate(compressedFrameData, Deflater.FULL_FLUSH);
            deflater.end();
            compressedFrameData.flip();

            sendBudget.accumulateAndGet(compressedFrameData.limit(), (budget, packetSize) -> budget - packetSize);
            final ProjectorFramebufferMessage message = new ProjectorFramebufferMessage(this, compressedFrameData);
            Network.sendToClientsTrackingChunk(message, chunk);
        });
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();

        tag.putBoolean(IS_PROJECTING_TAG_NAME, isProjecting);
        needsIDR = true;

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        super.handleUpdateTag(tag);

        setProjecting(tag.getBoolean(IS_PROJECTING_TAG_NAME));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(ENERGY_TAG_NAME, energy.serializeNBT());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);

        energy.deserializeNBT(tag.getCompound(ENERGY_TAG_NAME));
    }

    @Override
    public AABB getRenderBoundingBox() {
        return renderBounds;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBlockState(final BlockState state) {
        super.setBlockState(state);

        updateRenderBounds();
    }

    public void applyNextFrame(final ByteBuffer frameData) {
        final Future<?> lastDecode = runningDecode;
        runningDecode = FRAME_WORKERS.submit(() -> {
            try {
                joinWorkerAndLogErrors(lastDecode);

                final Inflater inflater = new Inflater();
                inflater.setInput(frameData);

                decoderBuffer.clear();
                inflater.inflate(decoderBuffer);
                decoderBuffer.flip();

                decoder.decodeFrame(decoderBuffer, picture.getData());

                synchronized (picture) {
                    if (frameConsumer != null) {
                        frameConsumer.processFrame(picture);
                    }
                }
            } catch (final DataFormatException ignored) {
            }
        });
    }

    ///////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (projectorsUseEnergy()) {
            collector.offer(Capabilities.ENERGY_STORAGE, energy);
        }
    }

    ///////////////////////////////////////////////////////////////

    private static boolean projectorsUseEnergy() {
        return Config.projectorEnergyStorage > 0 && Config.projectorEnergyPerTick > 0;
    }

    private void sendRunningState() {
        if (level != null && !level.isClientSide()) {
            Network.sendToClientsTrackingBlockEntity(new ProjectorStateMessage(this, isProjecting && hasEnergy), this);
        }
    }

    private void updateRenderBounds() {
        final Direction blockFacing = getBlockState().getValue(ProjectorBlock.FACING);
        final Direction canvasUp = Direction.UP;
        final Direction canvasLeft = blockFacing.getCounterClockWise();

        final BlockPos projectorPos = getBlockPos();
        final BlockPos screenBasePos = projectorPos.relative(blockFacing, MAX_RENDER_DISTANCE);
        final BlockPos screenMinPos = screenBasePos.relative(canvasLeft.getOpposite(), MAX_WIDTH / 2);
        final BlockPos screenMaxPos = screenBasePos.relative(canvasLeft, MAX_WIDTH / 2)
            // -1 for the MAX_HEIGHT padding, -1 for auto-expansion of AABB constructor
            .relative(canvasUp, MAX_HEIGHT - 2);

        renderBounds = new AABB(getBlockPos()).minmax(new AABB(screenMinPos)).minmax(new AABB(screenMaxPos));
    }

    private static void joinWorkerAndLogErrors(@Nullable final Future<?> job) {
        if (job == null) {
            return;
        }

        try {
            job.get();
        } catch (final InterruptedException ignored) {
        } catch (final ExecutionException e) {
            LOGGER.error(e);
        }
    }
}
