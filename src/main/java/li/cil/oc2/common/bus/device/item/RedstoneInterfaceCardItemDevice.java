package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.capabilities.RedstoneEmitter;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public final class RedstoneInterfaceCardItemDevice extends IdentityProxy<ItemStack> implements RPCDevice, ItemDevice, ICapabilityProvider {
    private static final int FACE_COUNT = Direction.values().length;

    private static final String OUTPUT_TAG_NAME = "output";

    ///////////////////////////////////////////////////////////////////

    private final TileEntity tileEntity;
    private final ObjectDevice device;
    private final RedstoneEmitter[] capabilities;
    private final byte[] output = new byte[FACE_COUNT];

    ///////////////////////////////////////////////////////////////////

    public RedstoneInterfaceCardItemDevice(final ItemStack value, final TileEntity tileEntity) {
        super(value);
        this.tileEntity = tileEntity;
        this.device = new ObjectDevice(this, "redstone");

        capabilities = new RedstoneEmitter[FACE_COUNT];
        for (int i = 0; i < FACE_COUNT; i++) {
            final int indexForClosure = i;
            capabilities[i] = () -> output[indexForClosure];
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<RPCMethod> getMethods() {
        return device.getMethods();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
        if (cap == Capabilities.REDSTONE_EMITTER && side != null) {
            return LazyOptional.of(() -> capabilities[side.getIndex()]).cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();
        nbt.putByteArray(OUTPUT_TAG_NAME, output);
        return nbt;
    }

    @Override
    public void deserializeNBT(final CompoundNBT nbt) {
        final byte[] serializedOutput = nbt.getByteArray(OUTPUT_TAG_NAME);
        System.arraycopy(serializedOutput, 0, output, 0, Math.min(serializedOutput.length, output.length));
    }

    ///////////////////////////////////////////////////////////////////

    @Callback(synchronize = false)
    public int getRedstoneOutput(final Direction side) {
        return output[side.getIndex()];
    }

    @Callback
    public void setRedstoneOutput(final Direction side, final int value) {
        final byte clampedValue = (byte) MathHelper.clamp(value, 0, 15);
        if (clampedValue == output[side.getIndex()]) {
            return;
        }

        output[side.getIndex()] = clampedValue;

        final Direction direction = HorizontalBlockUtils.toGlobal(tileEntity.getBlockState(), side);
        if (direction != null) {
            notifyNeighbors(direction);
        }
    }

    @Callback
    public int getRedstoneInput(final Direction side) {
        final World world = tileEntity.getWorld();
        if (world == null) {
            return 0;
        }

        final BlockPos pos = tileEntity.getPos();
        final Direction direction = HorizontalBlockUtils.toGlobal(tileEntity.getBlockState(), side);
        assert direction != null;

        final BlockPos neighborPos = pos.offset(direction);
        final ChunkPos chunkPos = new ChunkPos(neighborPos.getX(), neighborPos.getZ());
        if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
            return 0;
        }

        return world.getRedstonePower(neighborPos, direction);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final RedstoneInterfaceCardItemDevice that = (RedstoneInterfaceCardItemDevice) o;
        return tileEntity.equals(that.tileEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    private void notifyNeighbors(final Direction direction) {
        final World world = tileEntity.getWorld();
        if (world == null) {
            return;
        }

        final BlockPos pos = tileEntity.getPos();
        final BlockState state = tileEntity.getBlockState();
        final Block block = state.getBlock();

        world.notifyNeighborsOfStateChange(pos, block);
        world.notifyNeighborsOfStateChange(pos.offset(direction), block);
    }
}
