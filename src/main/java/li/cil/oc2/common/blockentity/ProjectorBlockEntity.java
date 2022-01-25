package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.bus.device.vm.ProjectorVMDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ProjectorFrameBufferTileMessage;
import li.cil.oc2.common.network.message.ProjectorStateMessage;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Optional;

public final class ProjectorBlockEntity extends ModBlockEntity {
    private static final int MAX_RENDER_DISTANCE = 12;
    private static final int MAX_WIDTH = MAX_RENDER_DISTANCE + 1; // +1 To make it odd, so we can center.
    private static final int MAX_HEIGHT = (MAX_RENDER_DISTANCE * ProjectorVMDevice.HEIGHT / ProjectorVMDevice.WIDTH) + 1; // + 1 To match horizontal margin.
    private static final LayerSize[] LAYER_SIZES = computeLayerSizes();

    private static final String ENERGY_TAG_NAME = "energy";
    private static final String IS_PROJECTING_TAG_NAME = "projecting";

    ///////////////////////////////////////////////////////////////

    private final ProjectorVMDevice projectorDevice = new ProjectorVMDevice(this);
    private boolean isProjecting, hasEnergy;
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.projectorEnergyStorage);

    // Client only data.
    private final BitSet dirtyLines = new BitSet(ProjectorVMDevice.HEIGHT);
    @Nullable private ByteBuffer buffer;
    private final BitSet[] visibilities = new BitSet[MAX_RENDER_DISTANCE];
    private AABB visibilityBounds;
    private AABB renderBounds;

    ///////////////////////////////////////////////////////////////

    public ProjectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.PROJECTOR.get(), pos, state);

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
            buffer = null;
            dirtyLines.set(0, ProjectorVMDevice.HEIGHT);
        }

        sendRunningState();
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();

        tag.putBoolean(IS_PROJECTING_TAG_NAME, isProjecting);
        projectorDevice.setAllDirty(); // todo is this good enough to be notified of new client observers?

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

    @FunctionalInterface
    public interface FramebufferPixelSetter {
        void set(final int x, final int y, final int rgba);
    }

    public boolean updateRenderTexture(final FramebufferPixelSetter setter) {
        if (dirtyLines.isEmpty()) {
            return false;
        }

        final ByteBuffer buffer = getOrCreateBuffer();
        for (int y = dirtyLines.nextSetBit(0); y >= 0; y = dirtyLines.nextSetBit(y + 1)) {
            for (int x = 0; x < ProjectorVMDevice.WIDTH; x++) {
                final int index = (x + y * ProjectorVMDevice.WIDTH) * Short.BYTES;
                final int r5g6b5 = buffer.getShort(index) & 0xFFFF;
                setter.set(x, y, ProjectorVMDevice.toRGBA(r5g6b5));
            }
        }

        dirtyLines.clear();
        return true;
    }

    public void applyFramebufferTile(final SimpleFramebufferDevice.Tile tile) {
        tile.apply(ProjectorVMDevice.WIDTH, getOrCreateBuffer());
        dirtyLines.set(tile.startPixelY(), tile.startPixelY() + SimpleFramebufferDevice.TILE_WIDTH);
    }

    ///////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @org.jetbrains.annotations.Nullable final Direction direction) {
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

        int byteBudget = Config.projectorMaxBytesPerTick;
        Optional<SimpleFramebufferDevice.Tile> tile;
        while (byteBudget > 0 && (tile = projectorDevice.getNextDirtyTile()).isPresent()) {
            final ProjectorFrameBufferTileMessage message = new ProjectorFrameBufferTileMessage(this, tile.get());
            Network.sendToClientsTrackingBlockEntity(message, this);
            byteBudget -= SimpleFramebufferDevice.TILE_SIZE_IN_BYTES;
        }
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

    private ByteBuffer getOrCreateBuffer() {
        if (buffer == null) {
            buffer = projectorDevice.allocateBuffer();
        }
        return buffer;
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
