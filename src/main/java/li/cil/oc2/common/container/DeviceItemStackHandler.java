package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.common.bus.device.provider.Providers;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DeviceItemStackHandler extends ItemStackHandler {
    private final HashSet<Device> devices = new HashSet<>();
    private final UUID[] deviceIds;

    public DeviceItemStackHandler(final int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public DeviceItemStackHandler(final NonNullList<ItemStack> stacks) {
        super(stacks);
        deviceIds = new UUID[stacks.size()];
        for (int i = 0; i < deviceIds.length; i++) {
            deviceIds[i] = UUID.randomUUID();
        }
    }

    @Override
    public boolean isItemValid(final int slot, @NotNull final ItemStack stack) {
        return super.isItemValid(slot, stack);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 1;
    }

    @Override
    protected void onLoad() {
        super.onLoad();

        final HashSet<Device> newDevices = new HashSet<>();
        for (int i = 0; i < getSlots(); i++) {
            final ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

//            final List<LazyOptional<Device>> devices = Providers.getDevices(stack);
        }
    }

    protected void onDevicesAdded(final Set<Device> devices) {

    }

    protected void onDevicesRemoved(final Set<Device> devices) {

    }
}
