package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.HorizontalDirectionalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;

public final class RedstoneInterfaceCardItemDevice extends IdentityProxy<ItemStack> implements RPCDevice, DocumentedDevice, ItemDevice, ICapabilityProvider {
    private static final String OUTPUT_TAG_NAME = "output";

    private static final String GET_REDSTONE_INPUT = "getRedstoneInput";
    private static final String GET_REDSTONE_OUTPUT = "getRedstoneOutput";
    private static final String SET_REDSTONE_OUTPUT = "setRedstoneOutput";
    private static final String SIDE = "side";
    private static final String VALUE = "value";

    ///////////////////////////////////////////////////////////////////

    private final BlockEntity tileEntity;
    private final ObjectDevice device;
    private final RedstoneEmitter[] capabilities;
    private final byte[] output = new byte[Constants.BLOCK_FACE_COUNT];

    ///////////////////////////////////////////////////////////////////

    public RedstoneInterfaceCardItemDevice(final ItemStack identity, final BlockEntity tileEntity) {
        super(identity);
        this.tileEntity = tileEntity;
        this.device = new ObjectDevice(this, "redstone");

        capabilities = new RedstoneEmitter[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final int indexForClosure = i;
            capabilities[i] = () -> output[indexForClosure];
        }
    }

    ///////////////////////////////////////////////////////////////////

    @NotNull
    @Override
    public <T> Optional<T> getCapability(@NotNull final Capability<T> capability, @Nullable final Direction side) {
        if (capability == Capabilities.REDSTONE_EMITTER && side != null) {
            return (Optional<T>) Optional.of(capabilities[side.get3DDataValue()]);
        }

        return Optional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putByteArray(OUTPUT_TAG_NAME, output);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        final byte[] serializedOutput = tag.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<RPCMethod> getMethods() {
        return device.getMethods();
    }

    @Callback(name = GET_REDSTONE_INPUT)
    public int getRedstoneInput(@Parameter(SIDE) final Direction side) {
        final Level world = tileEntity.getLevel();
        if (world == null) {
            return 0;
        }

        final BlockPos pos = tileEntity.getBlockPos();
        final Direction direction = HorizontalDirectionalBlockUtils.toGlobal(tileEntity.getBlockState(), side);
        assert direction != null;

        final BlockPos neighborPos = pos.relative(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos.getX(), neighborPos.getZ());
        if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return world.getSignal(neighborPos, direction);
    }

    @Callback(name = GET_REDSTONE_OUTPUT, synchronize = false)
    public int getRedstoneOutput(@Parameter(SIDE) final Direction side) {
        return output[side.get3DDataValue()];
    }

    @Callback(name = SET_REDSTONE_OUTPUT)
    public void setRedstoneOutput(@Parameter(SIDE) final Direction side, @Parameter(VALUE) final int value) {
        final byte clampedValue = (byte) Mth.clamp(value, 0, 15);
        if (clampedValue == output[side.get3DDataValue()]) {
            return;
        }

        output[side.get3DDataValue()] = clampedValue;

        final Direction direction = HorizontalDirectionalBlockUtils.toGlobal(tileEntity.getBlockState(), side);
        if (direction != null) {
            notifyNeighbor(direction);
        }
    }

    @Override
    public void getDeviceDocumentation(final DocumentedDevice.DeviceVisitor visitor) {
        visitor.visitCallback(GET_REDSTONE_INPUT)
                .description("Get the current redstone level received on the specified side. " +
                             "Note that if the current output level on the specified side is not " +
                             "zero, this will affect the measured level.\n" +
                             "Sides may be specified by name or zero-based index. Please note that" +
                             "the side depends on the orientation of the device's container.")
                .returnValueDescription("the current received level on the specified side.")
                .parameterDescription(SIDE, "the side to read the input level from.");

        visitor.visitCallback(GET_REDSTONE_OUTPUT)
                .description("Get the current redstone level transmitted on the specified side. " +
                             "This will return the value last set via setRedstoneOutput().\n" +
                             "Sides may be specified by name or zero-based index. Please note that" +
                             "the side depends on the orientation of the device's container.")
                .returnValueDescription("the current transmitted level on the specified side.")
                .parameterDescription(SIDE, "the side to read the output level from.");
        visitor.visitCallback(SET_REDSTONE_OUTPUT)
                .description("Set the new redstone level transmitted on the specified side.\n" +
                             "Sides may be specified by name or zero-based index. Please note that" +
                             "the side depends on the orientation of the device's container.")
                .parameterDescription(SIDE, "the side to write the output level to.")
                .parameterDescription(VALUE, "the output level to set, will be clamped to [0, 15].");
    }

    ///////////////////////////////////////////////////////////////////

    private void notifyNeighbor(final Direction direction) {
        final Level world = tileEntity.getLevel();
        if (world == null) {
            return;
        }

        world.updateNeighborsAt(tileEntity.getBlockPos(), tileEntity.getBlockState().getBlock());
        world.updateNeighborsAt(tileEntity.getBlockPos().relative(direction), tileEntity.getBlockState().getBlock());
    }
}
