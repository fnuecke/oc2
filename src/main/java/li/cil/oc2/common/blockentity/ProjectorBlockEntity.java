/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.ProjectorLoadBalancer;
import li.cil.oc2.common.network.message.ProjectorRequestFramebufferMessage;
import li.cil.oc2.common.network.message.ProjectorStateMessage;
import li.cil.oc2.jcodec.codecs.h264.H264Decoder;
import li.cil.oc2.jcodec.codecs.h264.H264Encoder;
import li.cil.oc2.jcodec.codecs.h264.encode.CQPRateControl;
import li.cil.oc2.jcodec.common.model.ColorSpace;
import li.cil.oc2.jcodec.common.model.Picture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class ProjectorBlockEntity extends ModBlockEntity implements TickableBlockEntity {
    @FunctionalInterface
    public interface FrameConsumer {
        void processFrame(final Picture picture);
    }

    ///////////////////////////////////////////////////////////////

    public static final int MAX_RENDER_DISTANCE = 16;
    public static final int MAX_GOOD_RENDER_DISTANCE = 12;
    public static final int MAX_WIDTH = MAX_GOOD_RENDER_DISTANCE + 1; // +1 To make it odd, so we can center.
    public static final int MAX_HEIGHT = (MAX_GOOD_RENDER_DISTANCE * ProjectorDevice.HEIGHT / ProjectorDevice.WIDTH) + 1; // + 1 To match horizontal margin.

    private static final String ENERGY_TAG_NAME = "energy";
    private static final String IS_PROJECTING_TAG_NAME = "projecting";

    private static final ExecutorService DECODER_WORKERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Projector Frame Decoder");
        return thread;
    });

    ///////////////////////////////////////////////////////////////

    private final ProjectorDevice projectorDevice = new ProjectorDevice(this);
    private boolean isProjecting, hasEnergy;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.projectorEnergyStorage);
    private final Picture picture = Picture.create(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, ColorSpace.YUV420J);

    // Video encoding.
    private final H264Encoder encoder = new H264Encoder(new CQPRateControl(12));
    private final ByteBuffer encoderBuffer = ByteBuffer.allocateDirect(1024 * 1024); // Re-used decompression buffer.
    private boolean needsIDR = true; // Whether we need to send a keyframe next.

    // Video decoding.
    private final H264Decoder decoder = new H264Decoder();
    @Nullable private CompletableFuture<?> runningDecode; // Current decoding operation, if any, to avoid race conditions.
    private final ByteBuffer decoderBuffer = ByteBuffer.allocateDirect(1024 * 1024); // Re-used decompression buffer.
    @Nullable private FrameConsumer frameConsumer; // Where to throw received frames.

    private AABB renderBounds; // Maximum possible render bounds, assuming we project on furthest away surface.
    private long lastKeepAliveSentAt;

    ///////////////////////////////////////////////////////////////

    public ProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PROJECTOR.get(), pos, state);

        encoder.setKeyInterval(100);

        updateRenderBounds();
    }

    ///////////////////////////////////////////////////////////////

    public boolean isProjecting() {
        if (!isProjecting || level == null) {
            return false;
        }

        final Direction facing = getBlockState().getValue(ProjectorBlock.FACING);
        final BlockPos neighborPos = getBlockPos().relative(facing);
        final int neighborChunkX = SectionPos.blockToSectionCoord(neighborPos.getX());
        final int neighborChunkZ = SectionPos.blockToSectionCoord(neighborPos.getZ());
        if (!level.hasChunk(neighborChunkX, neighborChunkZ)) {
            return false;
        }

        final BlockState neighborBlockState = level.getBlockState(neighborPos);
        return !neighborBlockState.isSolidRender(level, neighborPos);
    }

    public void setProjecting(final boolean value) {
        isProjecting = value;

        if (!isProjecting) {
            Arrays.fill(picture.getPlaneData(0), (byte) -128);
            Arrays.fill(picture.getPlaneData(1), (byte) 0);
            Arrays.fill(picture.getPlaneData(2), (byte) 0);
            needsIDR = true;
        }

        updateProjectorState();
    }

    public void setRequiresKeyframe() {
        needsIDR = true;
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

    public void onRendering() {
        final long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentAt > 1000) {
            lastKeepAliveSentAt = now;
            Network.sendToServer(new ProjectorRequestFramebufferMessage(this));
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
                updateProjectorState();
            }
            return;
        } else if (!hasEnergy) {
            hasEnergy = true;
            updateProjectorState();
        }

        if (!projectorDevice.hasChanges() && !needsIDR) {
            return;
        }

        ProjectorLoadBalancer.offerFrame(this, this::encodeFrame);
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
        final CompletableFuture<?> lastDecode = runningDecode;
        runningDecode = CompletableFuture.runAsync(() -> {
            try {
                try {
                    if (lastDecode != null) lastDecode.join();
                } catch (final CompletionException ignored) {
                }

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
        }, DECODER_WORKERS);
    }

    ///////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (projectorsUseEnergy()) {
            collector.offer(Capabilities.ENERGY_STORAGE, energy);
        }

        if (direction == getBlockState().getValue(ProjectorBlock.FACING).getOpposite()) {
            collector.offer(Capabilities.DEVICE, projectorDevice);
        }
    }

    ///////////////////////////////////////////////////////////////

    private static boolean projectorsUseEnergy() {
        return Config.projectorEnergyStorage > 0 && Config.projectorEnergyPerTick > 0;
    }

    private void updateProjectorState() {
        if (level != null && !level.isClientSide()) {
            level.setBlock(getBlockPos(), getBlockState().setValue(ProjectorBlock.LIT, isProjecting), Block.UPDATE_CLIENTS);

            Network.sendToClientsTrackingBlockEntity(new ProjectorStateMessage(this, isProjecting && hasEnergy), this);
        }
    }

    @Nullable
    private ByteBuffer encodeFrame() {
        final boolean hasChanges = projectorDevice.applyChanges(picture);
        if (!hasChanges && !needsIDR) {
            return null;
        }

        encoderBuffer.clear();
        final ByteBuffer frameData;
        try {
            if (needsIDR) {
                frameData = encoder.encodeIDRFrame(picture, encoderBuffer);
                needsIDR = false;
            } else {
                frameData = encoder.encodeFrame(picture, encoderBuffer).data();
            }
        } catch (final BufferOverflowException ignored) {
            return null;
        }

        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(frameData);
        deflater.finish();
        final ByteBuffer compressedFrameData = ByteBuffer.allocateDirect(1024 * 1024);
        deflater.deflate(compressedFrameData, Deflater.FULL_FLUSH);
        deflater.end();
        compressedFrameData.flip();

        return compressedFrameData;
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
}
