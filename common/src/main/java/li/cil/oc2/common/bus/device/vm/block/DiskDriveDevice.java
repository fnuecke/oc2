/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.provider.item.FloppyItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.FloppyControllerStorage;
import li.cil.oc2.common.bus.device.vm.item.FloppyMedia;
import li.cil.oc2.common.bus.device.vm.item.MappedStorage;
import li.cil.oc2.common.item.FloppyItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Objects;

public final class DiskDriveDevice extends AbstractRemovableMediaDevice {
    private final ArchitectureType architectureType;

    // --------------------------------------------------------------------- //

    public DiskDriveDevice(final RemovableMediaContainer container, final ArchitectureType architectureType) {
        super(container);
        this.architectureType = architectureType;
    }

    // --------------------------------------------------------------------- //

    public ArchitectureType getArchitectureType() {
        return architectureType;
    }

    @Override
    protected MappedStorage createStorage(final VMContext context) {
        return switch (architectureType) {
            case RISCV -> super.createStorage(context);
            case Z80 -> new FloppyControllerStorage();
        };
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean equals(@Nullable final Object o) {
        return super.equals(o) && architectureType == ((DiskDriveDevice) o).architectureType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), architectureType);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected int getMediumCapacity(final ItemStack stack) {
        if (!(stack.getItem() instanceof final FloppyItem floppy)) {
            return Math.min(Constants.FLOPPY_SIZE, Config.maxBlobCapacity);
        }

        final BlockDeviceData data = floppy.getData(stack);
        if (data != null) {
            return (int) Math.max(data.getBlockDevice().getCapacity(), 0);
        }

        return Math.min(floppy.getCapacity(stack), Config.maxBlobCapacity);
    }

    @Override
    protected boolean isSupportedMedium(final ItemStack stack) {
        return stack.getItem() instanceof FloppyItem;
    }

    @Override
    public String getDeviceDataKey() {
        return FloppyItemDeviceProvider.DEVICE_DATA_KEY;
    }

    @Override
    protected MediumInitializer createMediumInitializer(final ItemStack stack) {
        final BlockDeviceData data = ((FloppyItem) stack.getItem()).getData(stack);
        if (data == null) {
            return FloppyMedia::format;
        }

        return medium -> FloppyMedia.image(medium, data.getBlockDevice());
    }
}
