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
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
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
public final class ProjectorBlockEntity extends ModBlockEntity {
    @FunctionalInterface
    public interface FramebufferPixelSetter {
        void set(final int x, final int y, final int rgba);
    }

    ///////////////////////////////////////////////////////////////

    private static final int MAX_RENDER_DISTANCE = 12;
    private static final int MAX_WIDTH = MAX_RENDER_DISTANCE + 1; // +1 To make it odd, so we can center.
    private static final int MAX_HEIGHT = (MAX_RENDER_DISTANCE * ProjectorVMDevice.HEIGHT / ProjectorVMDevice.WIDTH) + 1; // + 1 To match horizontal margin.
    private static final LayerSize[] LAYER_SIZES = computeLayerSizes();

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
    private Future<?> runningEncode; // To allow waiting for previous frame to finish.
    private final Picture picture = Picture.create(ProjectorVMDevice.WIDTH, ProjectorVMDevice.HEIGHT, ColorSpace.YUV420J);

    private boolean needsIDR = true; // Whether we need to send a keyframe next.
    private final AtomicInteger sendBudget = new AtomicInteger(); // Remaining accumulated bandwidth budget.
    private int nextFrameIn = 0; // Remaining cooldown before sending next frame.

    // Client only data.
    private final H264Decoder decoder = new H264Decoder();
    private Future<?> runningDecode; // Current decoding operation, if any, to avoid race conditions.
    private final ByteBuffer decoderBuffer = ByteBuffer.allocateDirect(1024 * 1024); // Re-used decompression buffer.
    private volatile boolean isBufferDirty; // Whether buffer has changed and renderers need to update their texture.

    private final BitSet[] visibilities = new BitSet[MAX_RENDER_DISTANCE]; // Index of blocks we're projecting onto.
    private AABB visibilityBounds; // Bounds of all blocks we're projecting onto.
    private AABB renderBounds; // Overall render bounds, disregarding projection surface, to allow growing if necessary.

    ///////////////////////////////////////////////////////////////

    public ProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PROJECTOR.get(), pos, state);

        encoder.setKeyInterval(100);

        for (int i = 0; i < visibilities.length; i++) {
            visibilities[i] = new BitSet(MAX_WIDTH * MAX_HEIGHT);
        }
        visibilityBounds = super.getRenderBoundingBox();

        updateRenderBounds();
    }

    ///////////////////////////////////////////////////////////////

    public static void serverTick(final Level ignoredLevel, final BlockPos ignoredPos, final BlockState ignoredState, final ProjectorBlockEntity projector) {
        projector.serverTick();
    }

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
            isBufferDirty = true;
        }

        sendRunningState();
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

    public record LayerSize(float width, float height, int discreteWidth, int discreteHeight) { }

    public static LayerSize getLayerSize(final int distance) {
        return LAYER_SIZES[distance];
    }

    public record VisibilityData(AABB visibilityBounds, BitSet[] visibilities) { }

    public VisibilityData getVisibilityData() {
        updateVisibilities();
        return new VisibilityData(visibilityBounds, visibilities);
    }

    public boolean updateRenderTexture(final FramebufferPixelSetter setter) {
        assert level != null;
        final ProfilerFiller profiler = level.getProfiler();
        profiler.push("updateRenderTexture");

        if (!isBufferDirty) {
            profiler.pop();
            return false;
        }

        isBufferDirty = false;

        final byte[] y = picture.getPlaneData(0);
        final byte[] u = picture.getPlaneData(1);
        final byte[] v = picture.getPlaneData(2);

        // Convert in quads, based on the half resolution of UV. As such, skip every other row, since
        // we're setting the current and the next.
        int lumaIndex = 0, chromaIndex = 0;
        for (int halfRow = 0; halfRow < ProjectorVMDevice.HEIGHT / 2; halfRow++, lumaIndex += ProjectorVMDevice.WIDTH * 2) {
            final int row = halfRow * 2;
            for (int halfCol = 0; halfCol < ProjectorVMDevice.WIDTH / 2; halfCol++, chromaIndex++) {
                final int col = halfCol * 2;
                final int yIndex = lumaIndex + col;
                final byte cb = u[chromaIndex];
                final byte cr = v[chromaIndex];
                setFromYUV420(setter, col, row, y[yIndex], cb, cr);
                setFromYUV420(setter, col + 1, row, y[yIndex + 1], cb, cr);
                setFromYUV420(setter, col, row + 1, y[yIndex + ProjectorVMDevice.WIDTH], cb, cr);
                setFromYUV420(setter, col + 1, row + 1, y[yIndex + ProjectorVMDevice.WIDTH + 1], cb, cr);
            }
        }

        profiler.pop();

        return true;
    }

    public void applyNextFrame(final ByteBuffer frameData) {
        final Future<?> lastDecode = runningDecode;
        runningDecode = FRAME_WORKERS.submit(() -> {
            try {
                if (lastDecode != null) {
                    lastDecode.get();
                }

                final Inflater inflater = new Inflater();
                inflater.setInput(frameData);

                decoderBuffer.clear();
                inflater.inflate(decoderBuffer);
                decoderBuffer.flip();

                decoder.decodeFrame(decoderBuffer, picture.getData());

                isBufferDirty = true;
            } catch (final InterruptedException | ExecutionException | DataFormatException ignored) {
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

    private void serverTick() {
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

        nextFrameIn = FRAME_EVERY_N_TICKS;

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
            Network.sendToClientsTrackingBlockEntity(message, this);
        });
    }

    private void sendRunningState() {
        if (level != null && !level.isClientSide()) {
            Network.sendToClientsTrackingBlockEntity(new ProjectorStateMessage(this, isProjecting && hasEnergy), this);
        }
    }

    private void updateRenderBounds() {
        final Direction blockFacing = getBlockState().getValue(ProjectorBlock.FACING);
        final Direction screenUp = Direction.UP;
        final Direction screenLeft = blockFacing.getCounterClockWise();

        final BlockPos projectorPos = getBlockPos();
        final BlockPos screenBasePos = projectorPos.relative(blockFacing, MAX_RENDER_DISTANCE);
        final BlockPos screenMinPos = screenBasePos.relative(screenLeft.getOpposite(), MAX_WIDTH / 2);
        final BlockPos screenMaxPos = screenBasePos
            .relative(screenLeft, MAX_WIDTH / 2)
            // -1 for the MAX_HEIGHT padding, -1 for auto-expansion of AABB constructor
            .relative(screenUp, MAX_HEIGHT - 2);

        renderBounds = new AABB(getBlockPos()).minmax(new AABB(screenMinPos)).minmax(new AABB(screenMaxPos));
    }

    /**
     * Rebuild "stencil buffer" of blocks hit in projection direction, per block layer, up to max distance.
     */
    private void updateVisibilities() {
        final Direction blockFacing = getBlockState().getValue(ProjectorBlock.FACING);
        final Direction screenUp = Direction.UP;
        final Direction screenLeft = blockFacing.getCounterClockWise();

        final BlockPos projectorPos = getBlockPos();
        final BlockPos screenBasePos = projectorPos.relative(blockFacing, MAX_RENDER_DISTANCE + 1);
        final BlockPos screenOriginPos = screenBasePos.relative(screenLeft.getOpposite(), MAX_WIDTH / 2);

        final Vec3 toFaceCenter = new Vec3(blockFacing.getOpposite().step()).scale(0.45);
        final Vec3 clipStartPos = Vec3.atCenterOf(projectorPos.relative(blockFacing)).subtract(toFaceCenter);
        final Vec3 stepOrigin = Vec3.atCenterOf(screenOriginPos).add(toFaceCenter);
        final Vec3 upStep = new Vec3(screenUp.step());
        final Vec3 leftStep = new Vec3(screenLeft.step());

        for (final BitSet bitSet : visibilities) {
            bitSet.clear();
        }

        AABB bounds = new AABB(getBlockPos()).minmax(new AABB(getBlockPos().relative(blockFacing)));

        final Level level = getLevel();
        if (level == null) {
            return;
        }

        for (int y = 0; y < MAX_HEIGHT + 1; y++) {
            for (int x = 0; x < MAX_WIDTH; x++) {
                final Vec3 clipEndPos = stepOrigin.add(upStep.scale(y)).add(leftStep.scale(x));
                final ClipContext context = new ClipContext(clipStartPos, clipEndPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null);
                final BlockHitResult hit = level.clip(context);
                if (hit.getType() == HitResult.Type.MISS) {
                    continue;
                }

                if (hit.getDirection() != blockFacing.getOpposite()) {
                    continue;
                }

                final BlockState blockState = level.getBlockState(hit.getBlockPos());
                if (!blockState.isFaceSturdy(level, hit.getBlockPos(), blockFacing.getOpposite())) {
                    continue;
                }

                final BlockPos delta = hit.getBlockPos().subtract(projectorPos);
                final int distance = Math.abs(delta.get(blockFacing.getAxis())) - 2;
                if (distance >= visibilities.length) {
                    continue;
                }

                final int globalX = delta.get(screenLeft.getAxis());
                final int globalY = delta.get(screenUp.getAxis());

                final ProjectorBlockEntity.LayerSize layerSize = getLayerSize(distance);
                final int discreteWidth = layerSize.discreteWidth();
                final int discreteHeight = layerSize.discreteHeight();

                if (globalY < 0 || globalY >= discreteHeight) {
                    continue;
                }

                final int localX = globalX + discreteWidth / 2;
                if (localX < 0 || localX >= discreteWidth) {
                    continue;
                }

                bounds = bounds.minmax(new AABB(hit.getBlockPos()));

                final int index = localX + globalY * discreteWidth;
                visibilities[distance].set(index);
            }
        }

        visibilityBounds = bounds;
    }

    private static final ThreadLocal<byte[]> rgb = ThreadLocal.withInitial(() -> new byte[3]);

    private static void setFromYUV420(final FramebufferPixelSetter setter, final int col, final int row, final byte y, final byte cb, final byte cr) {
        final byte[] bytes = rgb.get();
        Yuv420jToRgb.YUVJtoRGB(y, cb, cr, bytes, 0);
        final int r = bytes[0] + 128;
        final int g = bytes[1] + 128;
        final int b = bytes[2] + 128;
        setter.set(col, row, r | (g << 8) | (b << 16) | (0xFF << 24));
    }

    private static LayerSize[] computeLayerSizes() {
        final LayerSize[] layerSizes = new LayerSize[MAX_RENDER_DISTANCE];
        for (int distance = 0; distance < layerSizes.length; distance++) {
            final float bufferWidth = ProjectorVMDevice.WIDTH;
            final float bufferHeight = ProjectorVMDevice.HEIGHT;
            final float ratio = bufferHeight / bufferWidth;
            final float width = (MAX_WIDTH - 1) * (float) (distance + 1) / MAX_RENDER_DISTANCE;
            final float height = width * ratio;
            int discreteWidth = (int) Math.ceil(width);
            discreteWidth += 1 - (discreteWidth & 1); // we center, so even values eat up one more
            int discreteHeight = (int) Math.ceil(height); // we align, so actual height is correct
            if (Math.abs(discreteHeight - height) < 0.001) discreteHeight++;
            layerSizes[distance] = new LayerSize(width, height, discreteWidth, discreteHeight);
        }
        return layerSizes;
    }
}
