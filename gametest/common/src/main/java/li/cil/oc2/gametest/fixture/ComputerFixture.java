/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fixture;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.gametest.util.TestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import static li.cil.oc2.gametest.util.TestSupport.COMPUTER_POS;
import static li.cil.oc2.gametest.util.TestSupport.fakePlayer;

public final class ComputerFixture implements MachineFixture {
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

    public static ComputerFixture placePowered(final GameTestHelper helper, final BlockPos pos) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = place(helper, player, pos);
        TestSupport.place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), pos.west());
        return computer;
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

    @Override
    public Terminal terminal() {
        return blockEntity().getTerminal();
    }

    public ComputerBlockEntity blockEntity() {
        final ComputerBlockEntity blockEntity = helper.getBlockEntity(pos);
        if (blockEntity == null) {
            throw new GameTestAssertException("no computer at " + pos);
        }
        return blockEntity;
    }

    @Override
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

    public void assertBootError(final String key, final String what) {
        final Component expected = Component.translatable(key);
        if (!expected.equals(virtualMachine().getBootError())) {
            throw new GameTestAssertException(what + ": computer reports boot error "
                + virtualMachine().getBootError() + ", expected " + expected);
        }
    }

    public void assertNoBootError() {
        if (virtualMachine().getBootError() != null) {
            throw new GameTestAssertException("computer reports boot error " + virtualMachine().getBootError());
        }
    }

    // --------------------------------------------------------------------- //

    @Override
    public ItemHandler handler(final DeviceType type) {
        return blockEntity().getItemStackHandlers().getItemHandler(type)
            .orElseThrow(() -> new GameTestAssertException("no item handler for " + type));
    }

    public ItemStack slot(final DeviceType type) {
        return handler(type).getStackInSlot(0);
    }

    @Override
    public ComputerFixture install(final DeviceType type, final ItemStack stack) {
        installInto(type, stack);
        return this;
    }

    public ItemStack uninstall(final DeviceType type) {
        final ItemStack removed = handler(type).extractItem(0, 1, false);
        if (removed.isEmpty()) {
            throw new GameTestAssertException("nothing to remove from the " + type + " slot");
        }
        return removed;
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

    @Override
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
