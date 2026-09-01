/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static li.cil.oc2.gametest.TestSupport.COMPUTER_POS;
import static li.cil.oc2.gametest.TestSupport.fakePlayer;

public final class ComputerFixture {
    private final GameTestHelper helper;
    private final BlockPos pos;

    // --------------------------------------------------------------------- //

    public static ComputerFixture place(final GameTestHelper helper) {
        return place(helper, fakePlayer(helper), COMPUTER_POS);
    }

    public static ComputerFixture place(final GameTestHelper helper, final Player player) {
        return place(helper, player, COMPUTER_POS);
    }

    public static ComputerFixture place(final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.place(helper, player, new ItemStack(Items.COMPUTER.get()), pos);
        return new ComputerFixture(helper, pos);
    }

    public static ComputerFixture at(final GameTestHelper helper, final BlockPos pos) {
        return new ComputerFixture(helper, pos);
    }

    public static ComputerFixture at(final GameTestHelper helper) {
        return new ComputerFixture(helper, COMPUTER_POS);
    }

    // --------------------------------------------------------------------- //

    public BlockPos pos() {
        return pos;
    }

    public ComputerBlockEntity blockEntity() {
        return helper.getBlockEntity(pos);
    }

    public VirtualMachine virtualMachine() {
        return blockEntity().getVirtualMachine();
    }

    public VMRunState runState() {
        return virtualMachine().getRunState();
    }

    public void start() {
        blockEntity().start();
    }

    public void stop() {
        blockEntity().stop();
    }

    public void assertRunState(final VMRunState expected, final String what) {
        final VirtualMachine vm = virtualMachine();
        if (vm.getRunState() != expected) {
            throw new GameTestAssertException(what + ": computer is " + vm.getRunState()
                + ", expected " + expected + ", bootError=" + vm.getBootError());
        }
    }

    public void assertNoBootError() {
        if (virtualMachine().getBootError() != null) {
            throw new GameTestAssertException("computer reports boot error " + virtualMachine().getBootError());
        }
    }

    public void assertNoError() {
        if (virtualMachine().getError() != null) {
            throw new GameTestAssertException("VM reported an error: " + virtualMachine().getError());
        }
    }

    // --------------------------------------------------------------------- //

    public ItemHandler handler(final DeviceType type) {
        return blockEntity().getItemStackHandlers().getItemHandler(type)
            .orElseThrow(() -> new GameTestAssertException("no item handler for " + type));
    }

    public ItemStack slot(final DeviceType type) {
        return handler(type).getStackInSlot(0);
    }

    public ComputerFixture install(final DeviceType type, final ItemStack stack) {
        final ItemHandler handler = handler(type);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack, false).isEmpty()) {
                return this;
            }
        }
        throw new GameTestAssertException("could not install " + stack + " as " + type
            + "; all " + handler.getSlots() + " slot(s) rejected it");
    }

    public ItemStack uninstall(final DeviceType type) {
        final ItemStack removed = handler(type).extractItem(0, 1, false);
        if (removed.isEmpty()) {
            throw new GameTestAssertException("nothing to remove from the " + type + " slot");
        }
        return removed;
    }

    public Set<Device> devices() {
        return ((AbstractVirtualMachine) virtualMachine()).getBusController().getDevices();
    }

    public int deviceCount() {
        return devices().size();
    }

    @Nullable
    public NetworkInterface networkInterface(final Direction side) {
        return Capabilities.get(blockEntity(), Capabilities.NETWORK_INTERFACE, side);
    }

    public long energy() {
        final var storage = Capabilities.get(blockEntity(), Capabilities.ENERGY_STORAGE, null);
        if (storage == null) {
            throw new GameTestAssertException("computer exposes no energy storage capability");
        }
        return storage.getEnergyStored();
    }

    // --------------------------------------------------------------------- //

    public CompoundTag save() {
        return blockEntity().saveWithFullMetadata(registries());
    }

    public CompoundTag updateTag() {
        return blockEntity().getUpdateTag(registries());
    }

    public void load(final CompoundTag tag) {
        blockEntity().loadWithComponents(tag, registries());
    }

    // --------------------------------------------------------------------- //

    public String screen() {
        final Terminal terminal = blockEntity().getTerminal();
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

    public void type(final String text) {
        final Terminal terminal = blockEntity().getTerminal();
        for (final byte value : text.getBytes(StandardCharsets.US_ASCII)) {
            terminal.putInput(value);
        }
    }

    public void assertScreenContains(final String expected, final String what) {
        final String text = screen();
        if (!text.contains(expected)) {
            throw new GameTestAssertException(what + ": screen does not hold [" + expected + "]; "
                + describe() + "\n" + text);
        }
    }

    public String describe() {
        final VirtualMachine vm = virtualMachine();
        return "runState=" + vm.getRunState()
            + ", bootError=" + vm.getBootError()
            + ", error=" + vm.getError()
            + ", devices=" + deviceCount()
            + ", cycles=" + guestInstructions()
            + ", energy=" + energy()
            + ", deviceList=" + devices().stream().map(d -> d.getClass().getSimpleName()).sorted().toList();
    }

    public GuestTests guestTests() {
        return GuestTests.of(virtualMachine());
    }

    public void assertNoGuestPanic() {
        final String text = screen();
        for (final String marker : new String[]{"Kernel panic", "Oops", "BUG:", "Call Trace"}) {
            if (text.contains(marker)) {
                throw new GameTestAssertException("guest reported '" + marker + "':\n" + text);
            }
        }
    }

    public long guestInstructions() {
        return ((AbstractVirtualMachine) virtualMachine()).getInstructionsRetired();
    }

    // --------------------------------------------------------------------- //

    private RegistryAccess registries() {
        return helper.getLevel().registryAccess();
    }

    // --------------------------------------------------------------------- //

    private ComputerFixture(final GameTestHelper helper, final BlockPos pos) {
        this.helper = helper;
        this.pos = pos;
    }
}
