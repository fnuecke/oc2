/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.SystemDevice;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;

@RPCDeviceDescription(typeNames = {"scanner"}, description = """
    Provided by the [scanner module](../item/scanner_module.md) to robots.

    ### Sides
    The side parameter of `inspect()` represents a face of the robot. Valid values are: `front`, `up` and `down`.

    ### Scanning
    `scan()` measures the hardness of every block in a cube of seven by seven by seven blocks with the robot at its center. Positions use the axes of the robot's `getPosition()`: `x` grows towards east, `y` upward and `z` towards south.

    The result holds one byte per block. Blocks are ordered by `y`, then `z`, then `x`, each from `-3` to `3`, so the result is a stack of horizontal layers from bottom to top, and `x` changes fastest. The block at offset `x`, `y`, `z` from the robot is at index `(y + 3) * 7 * 7 + (z + 3) * 7 + (x + 3)`, counting from zero. Since Lua counts from one, this is equivalent to `string.byte(hardness, index + 1)`.

    Each byte encodes air as `0`, any fluid as `254` and unbreakable or unscannable blocks as `255`. Regular blocks are encoded as hardness mapped into the `1` to `253` range.

    After a scan, the scanner needs five seconds to recharge. Scanning while it recharges fails.

    ### Fluids
    Fluids are always reported by their source fluid, such as `minecraft:water`.

    ### Entities
    Entities are reported by identifier, such as `minecraft:sheep`, closest first. At most twenty are listed.""")
@IODeviceDescription(name = "SCANNR", description = """
    Sides are numbered: `0` front, `1` up and `2` down. Block and fluid ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup. Block `0` is air, fluid `0` is no fluid.

    A scan is read one layer at a time. Layers are horizontal and numbered `0` to `6` from bottom to top, each holds 49 bytes ordered from north to south, then from west to east. Byte values are as described for the high-level API.""")
public final class ScannerModuleDevice extends AbstractItemDevice {
    public record Inspection(@Nullable String block, @Nullable String fluid, String[] entities) {
    }

    public record ScanResult(byte[] hardness, String[] entities) {
    }

    // --------------------------------------------------------------------- //

    private static final String READY_AT_TAG_NAME = "ready_at";
    private static final String LAST_SCAN_TAG_NAME = "last_scan";

    private static final int INSPECT_CODE = 1;
    private static final int SCAN_CODE = 2;
    private static final int GET_SCAN_LAYER_CODE = 3;
    private static final int CAN_SEE_SKY_CODE = 4;

    private static final int SCAN_RADIUS = 3;
    private static final int SCAN_LENGTH = SCAN_RADIUS * 2 + 1;
    private static final int SCAN_LAYER_SIZE = SCAN_LENGTH * SCAN_LENGTH;
    private static final int SCAN_SIZE = SCAN_LAYER_SIZE * SCAN_LENGTH;
    private static final int SCAN_COOLDOWN = TickUtils.toTicks(Duration.ofSeconds(5));
    private static final int MAX_ENTITIES = 20;

    private static final int HARDNESS_AIR = 0;
    private static final int HARDNESS_MAX = 253;
    private static final int HARDNESS_FLUID = 254;
    private static final int HARDNESS_UNBREAKABLE = 255;

    private static final String INSPECT_DESCRIPTION = "Identifies the block, fluid and entities in the space on the specified side of the robot.";
    private static final String SCAN_DESCRIPTION = "Measures the hardness of the blocks around the robot and lists nearby entities.";
    private static final String CAN_SEE_SKY_DESCRIPTION = "Checks whether the robot's space gets full sky light.";

    // --------------------------------------------------------------------- //

    private final Entity entity;
    private long readyAt;
    @Nullable
    private byte[] lastScan;

    // --------------------------------------------------------------------- //

    public ScannerModuleDevice(final ItemStack identity, final Entity entity) {
        super(identity);
        this.entity = entity;
    }

    // --------------------------------------------------------------------- //

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(READY_AT_TAG_NAME, readyAt);
        if (lastScan != null) {
            tag.putByteArray(LAST_SCAN_TAG_NAME, lastScan);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        readyAt = tag.getLong(READY_AT_TAG_NAME);
        lastScan = tag.contains(LAST_SCAN_TAG_NAME) ? tag.getByteArray(LAST_SCAN_TAG_NAME) : null;
    }

    // --------------------------------------------------------------------- //

    @Callback(description = INSPECT_DESCRIPTION, energy = 1,
        returnValueDescription = "a table with the `block` and `fluid`, if present, and a list of `entities`.")
    public Inspection inspect(@Parameter(value = "side", description = "the side to look at: `front`, `up` or `down`.") @Nullable final RobotOperationSide side) {
        final BlockPos pos = getAdjacent(side);
        final BlockState state = getBlockState(pos);
        final FluidState fluid = state.getFluidState();
        return new Inspection(
            state.isAir() ? null : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
            fluid.isEmpty() ? null : BuiltInRegistries.FLUID.getKey(getSourceFluid(fluid)).toString(),
            getEntities(new AABB(pos)));
    }

    @Callback(description = SCAN_DESCRIPTION, energy = 10,
        returnValueDescription = "a table with the block `hardness` as binary data, see above, and the list of " +
            "`entities` in the scanned cube.")
    public ScanResult scan() {
        final Level level = entity.level();
        if (level.getGameTime() < readyAt) {
            throw new IllegalStateException("scanner is recharging");
        }
        readyAt = level.getGameTime() + SCAN_COOLDOWN;

        final BlockPos center = getCenter();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final byte[] hardness = new byte[SCAN_SIZE];
        int index = 0;
        for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
                    pos.setWithOffset(center, x, y, z);
                    hardness[index++] = (byte) getHardness(level, pos);
                }
            }
        }

        lastScan = hardness;
        return new ScanResult(hardness, getEntities(new AABB(center).inflate(SCAN_RADIUS)));
    }

    @Callback(description = CAN_SEE_SKY_DESCRIPTION,
        returnValueDescription = "whether the sky is visible.")
    public boolean canSeeSky() {
        return entity.level().canSeeSky(getCenter());
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = INSPECT_CODE, energy = 1,
        description = INSPECT_DESCRIPTION,
        argumentsDescription = "one byte, the side.",
        resultsDescription = "five bytes: the block as two bytes, the fluid as two bytes, and the number of entities.")
    public void inspect(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final BlockPos pos = getAdjacent(RobotOperationSide.byIndex(arguments.readU8()));
        final BlockState state = getBlockState(pos);
        results.writeU16(state.isAir() ? 0 : SystemDevice.toId(BuiltInRegistries.BLOCK, state.getBlock()));
        results.writeU16(SystemDevice.toId(BuiltInRegistries.FLUID, getSourceFluid(state.getFluidState())));
        results.writeU8(getEntities(new AABB(pos)).length);
    }

    @IOCallback(value = SCAN_CODE, energy = 10,
        description = SCAN_DESCRIPTION + " Read the result with `getScanLayer`.",
        resultsDescription = "one byte, the number of entities in the scanned cube.")
    public void scan(final IOOutputStream results) throws IOException {
        results.writeU8(scan().entities().length);
    }

    @IOCallback(value = GET_SCAN_LAYER_CODE,
        description = "Reads one layer of the last scan. Fails if there was no scan yet.",
        argumentsDescription = "one byte, the layer.",
        resultsDescription = "49 bytes, the hardness of each block in the layer.")
    public void getScanLayer(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int layer = arguments.readU8();
        if (layer >= SCAN_LENGTH) {
            throw new IllegalArgumentException("layer out of range: " + layer + " (expected 0 to " + (SCAN_LENGTH - 1) + ")");
        }
        if (lastScan == null) {
            throw new IllegalStateException("no scan yet");
        }
        results.write(lastScan, layer * SCAN_LAYER_SIZE, SCAN_LAYER_SIZE);
    }

    @IOCallback(value = CAN_SEE_SKY_CODE,
        description = CAN_SEE_SKY_DESCRIPTION,
        resultsDescription = "one byte, `1` if the sky is visible, `0` otherwise.")
    public void canSeeSky(final IOOutputStream results) throws IOException {
        results.writeU8(canSeeSky() ? 1 : 0);
    }

    // --------------------------------------------------------------------- //

    private BlockPos getCenter() {
        return BlockPos.containing(entity.getBoundingBox().getCenter());
    }

    private BlockPos getAdjacent(@Nullable final RobotOperationSide side) {
        if (side == null) throw new IllegalArgumentException();
        return entity.blockPosition().relative(RobotOperationSide.toGlobal(entity, side));
    }

    private BlockState getBlockState(final BlockPos pos) {
        final Level level = entity.level();
        return isLoaded(level, pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    private String[] getEntities(final AABB bounds) {
        return entity.level().getEntities(entity, bounds, EntitySelector.NO_SPECTATORS).stream()
            .sorted(Comparator.comparingDouble(other -> other.distanceToSqr(entity)))
            .limit(MAX_ENTITIES)
            .map(other -> BuiltInRegistries.ENTITY_TYPE.getKey(other.getType()).toString())
            .toArray(String[]::new);
    }

    @SuppressWarnings("deprecation")
    private static int getHardness(final Level level, final BlockPos pos) {
        if (!isLoaded(level, pos)) {
            return HARDNESS_UNBREAKABLE;
        }

        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return HARDNESS_AIR;
        }
        if (state.liquid() || state.getBlock() instanceof LiquidBlock) {
            return HARDNESS_FLUID;
        }

        final float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) {
            return HARDNESS_UNBREAKABLE;
        }
        return Math.clamp(Math.round(hardness * 10), 1, HARDNESS_MAX);
    }

    private static Fluid getSourceFluid(final FluidState fluid) {
        return fluid.getType() instanceof final FlowingFluid flowing ? flowing.getSource() : fluid.getType();
    }

    private static boolean isLoaded(final Level level, final BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
