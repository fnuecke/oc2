/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.bus.device.RedstoneInterfaceDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public final class RedstoneInterfaceBlockEntity extends ModBlockEntity {
    private final RedstoneInterfaceDevice redstoneInterface = new RedstoneInterfaceDevice(this);
    private final ObjectDevice device = new ObjectDevice(redstoneInterface);

    // --------------------------------------------------------------------- //

    public RedstoneInterfaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.REDSTONE_INTERFACE.get(), pos, state);
    }

    public void handleNeighborChanged() {
        redstoneInterface.handleNeighborChanged();
    }

    public int getOutputForDirection(final Direction direction) {
        return redstoneInterface.getOutput(HorizontalBlockUtils.toLocal(getBlockState(), direction));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);

        redstoneInterface.save(output);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);

        redstoneInterface.load(input);
    }

    @Override
    protected void loadServerInLoadedLevel() {
        super.loadServerInLoadedLevel();

        redstoneInterface.refreshInputs();
    }

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.DEVICE, device);
    }
}
