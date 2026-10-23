/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.sedna.device.virtio.VirtIOKeyboardDevice;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

public final class KeyboardDevice<T> extends IdentityProxy<T> implements VMDevice, TerminalUserProvider {
    private static final String DEVICE_TAG_NAME = "device";
    private static final String ADDRESS_TAG_NAME = "address";
    private static final String INTERRUPT_TAG_NAME = "interrupt";
    private static final long USER_EXPIRES_AFTER = 2000; // milliseconds
    public static final long USER_KEEPALIVE_EVERY = 1000; // milliseconds

    // --------------------------------------------------------------------- //

    @Nullable
    private VirtIOKeyboardDevice device;

    // --------------------------------------------------------------------- //

    private final Map<Player, Long> users = new WeakHashMap<>();
    private final OptionalAddress address = new OptionalAddress();
    private final OptionalInterrupt interrupt = new OptionalInterrupt();
    private CompoundTag deviceTag;

    // --------------------------------------------------------------------- //

    public KeyboardDevice(final T identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    public void sendKeyEvent(final int keycode, final boolean isDown) {
        if (device != null) {
            device.sendKeyEvent(keycode, isDown);
        }
    }

    public void handleUsedBy(final Player player) {
        users.put(player, System.currentTimeMillis());
    }

    @Override
    public Iterable<Player> getTerminalUsers() {
        final long now = System.currentTimeMillis();
        users.entrySet().removeIf(entry -> now - entry.getValue() > USER_EXPIRES_AFTER);

        return new ArrayList<>(users.keySet());
    }

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        if (!allocateDevice(context)) {
            return VMDeviceLoadResult.fail();
        }

        assert device != null;
        if (!address.claim(context.getDeviceRangeAllocator(), device)) {
            return VMDeviceLoadResult.fail()
                .withErrorMessage(Component.translatable(Constants.COMPUTER_ERROR_DEVICE_DOES_NOT_FIT));
        }

        if (interrupt.claim(context)) {
            device.getInterrupt().set(interrupt.getAsInt(), context.getInterruptController());
        } else {
            return VMDeviceLoadResult.fail();
        }

        context.getEventBus().register(this);

        if (deviceTag != null) {
            NBTSerialization.deserialize(deviceTag, device);
        }

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }

        device = null;
        users.clear();
    }

    @Override
    public void dispose() {
        deviceTag = null;
        address.clear();
        interrupt.clear();
    }

    @Override
    public void serialize(final ValueOutput output) {
        if (device != null) {
            deviceTag = NBTSerialization.serialize(device);
        }
        if (deviceTag != null) {
            output.store(DEVICE_TAG_NAME, CompoundTag.CODEC, deviceTag);
        }
        if (address.isPresent()) {
            output.putLong(ADDRESS_TAG_NAME, address.getAsLong());
        }
        if (interrupt.isPresent()) {
            output.putInt(INTERRUPT_TAG_NAME, interrupt.getAsInt());
        }
    }

    @Override
    public void deserialize(final ValueInput input) {
        input.read(DEVICE_TAG_NAME, CompoundTag.CODEC).ifPresent(tag -> deviceTag = tag);
        input.getLong(ADDRESS_TAG_NAME).ifPresent(address::set);
        input.getInt(INTERRUPT_TAG_NAME).ifPresent(interrupt::set);
    }

    // --------------------------------------------------------------------- //

    private boolean allocateDevice(final VMContext context) {
        if (!context.getMemoryAllocator().claimMemory(Constants.PAGE_SIZE)) {
            return false;
        }

        device = new VirtIOKeyboardDevice(context.getMemoryMap(), Constants.VIRTIO_INPUT_QUEUE_SIZE);

        return true;
    }
}
