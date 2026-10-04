/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fixture;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.device.Terminal;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MachineFixture {
    String[] GUEST_PANIC_MARKERS = {"Kernel panic", "Oops", "BUG:", "Call Trace"};
    String LOGIN_PROMPT = "login:";
    String SHELL_PROMPT = "# ";

    VirtualMachine virtualMachine();

    Terminal terminal();

    ItemHandler handler(DeviceType type);

    String describe();

    MachineFixture install(DeviceType type, ItemStack stack);

    // --------------------------------------------------------------------- //

    default void assertRunState(final VMRunState expected, final String what) {
        final VirtualMachine vm = virtualMachine();
        if (vm.getRunState() != expected) {
            throw new GameTestAssertException(what + ": run state is " + vm.getRunState()
                + ", expected " + expected + "; " + describe());
        }
    }

    default void assertNoError() {
        if (virtualMachine().getError() != null) {
            throw new GameTestAssertException("VM reported an error: " + virtualMachine().getError());
        }
    }

    default Set<Device> devices() {
        return ((AbstractVirtualMachine) virtualMachine()).getBusController().getDevices();
    }

    default IODevice ioDevice(final String name) {
        final List<IODevice> matches = devices().stream()
            .filter(device -> device instanceof final IODevice io && name.equals(io.getIOName()))
            .map(IODevice.class::cast)
            .toList();
        if (matches.size() != 1) {
            throw new GameTestAssertException("expected one device providing the " + name + " mid-level API, found " + matches.size());
        }
        return matches.getFirst();
    }

    default long guestInstructions() {
        return ((AbstractVirtualMachine) virtualMachine()).getInstructionsRetired();
    }

    default GuestTests guestTests() {
        return GuestTests.of(virtualMachine());
    }

    static String screen(final Terminal terminal) {
        final CompoundTag tag;
        synchronized (terminal) {
            tag = NBTSerialization.serialize(terminal);
        }

        final byte[] buffer = tag.getByteArray("buffer");
        final StringBuilder text = new StringBuilder();
        for (int row = 0; row < Terminal.HEIGHT; row++) {
            for (int column = 0; column < Terminal.WIDTH; column++) {
                final int index = row * Terminal.WIDTH + column;
                final byte value = index < buffer.length ? buffer[index] : 0;
                text.append(value == 0 ? ' ' : (char) (value & 0xFF));
            }
            text.append('\n');
        }
        return text.toString();
    }

    default String screen() {
        return screen(terminal());
    }

    default void type(final String text) {
        final Terminal terminal = terminal();
        for (final byte value : text.getBytes(StandardCharsets.US_ASCII)) {
            terminal.putInput(value);
        }
    }

    default void loginAsRoot() {
        type("root\n");
    }

    default void assertScreenContains(final String expected, final String what) {
        final String text = screen();
        if (!text.contains(expected)) {
            throw new GameTestAssertException(what + ": screen does not hold [" + expected + "]; "
                + describe() + "\n" + text);
        }
    }

    default Matcher assertScreenMatches(final Pattern pattern, final String what) {
        final String text = screen();
        final Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new GameTestAssertException(what + ": screen does not match [" + pattern + "]; "
                + describe() + "\n" + text);
        }
        return matcher;
    }

    default void assertNoGuestPanic() {
        final String text = screen();
        for (final String marker : GUEST_PANIC_MARKERS) {
            if (text.contains(marker)) {
                throw new GameTestAssertException("guest reported '" + marker + "':\n" + text);
            }
        }
    }

    // --------------------------------------------------------------------- //

    default void installInto(final DeviceType type, final ItemStack stack) {
        final ItemHandler handler = handler(type);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack, false).isEmpty()) {
                return;
            }
        }
        throw new GameTestAssertException("could not install " + stack + " as " + type
            + "; all " + handler.getSlots() + " slot(s) rejected it");
    }
}
