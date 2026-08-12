/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import io.netty.buffer.Unpooled;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemSyncSizeTests {
    private static final int MAX_DEVICE_STACK_BYTES = 256;
    private static final int MAX_COMPUTER_STACK_BYTES = 2048;

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void storageItemsSyncByHandleNotByPayload(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), POWER_POS);
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), CABLE_POS);
        useOn(helper, player, new ItemStack(Items.BUS_INTERFACE.get()), CABLE_POS, Direction.WEST);
        useOn(helper, player, new ItemStack(Items.BUS_INTERFACE.get()), CABLE_POS, Direction.EAST);
        player.setYRot(90);
        place(helper, player, new ItemStack(Items.DISK_DRIVE.get()), DEVICE_POS);

        helper.startSequence()
                .thenExecuteAfter(60, () -> {
                    insert(helper, DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()));
                    insert(helper, DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()));
                    insert(helper, DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_CUSTOM.get()));
                    diskDrive(helper).insert(new ItemStack(Items.FLOPPY.get()), null);
                })
                .thenExecuteAfter(20, () -> computer(helper).start())
                .thenExecuteAfter(300, () -> {
                    final var vm = computer(helper).getVirtualMachine();
                    if (vm.getRunState() != VMRunState.RUNNING) {
                        throw new GameTestAssertException("precondition: computer did not reach RUNNING, was "
                                + vm.getRunState() + ", bootError=" + vm.getBootError());
                    }

                    final RegistryAccess registries = helper.getLevel().registryAccess();

                    final ItemStack hardDrive = slot(helper, DeviceTypes.HARD_DRIVE);
                    final ItemStack floppy = diskDrive(helper).getFloppy();

                    assertBlobHandle("hard drive", hardDrive);
                    assertBlobHandle("floppy", floppy);

                    assertSyncSize(registries, "flash memory",
                            slot(helper, DeviceTypes.FLASH_MEMORY), MAX_DEVICE_STACK_BYTES);
                    assertSyncSize(registries, "memory",
                            slot(helper, DeviceTypes.MEMORY), MAX_DEVICE_STACK_BYTES);
                    assertSyncSize(registries, "hard drive", hardDrive, MAX_DEVICE_STACK_BYTES);
                    assertSyncSize(registries, "floppy", floppy, MAX_DEVICE_STACK_BYTES);

                    // Breaking a computer folds its whole device inventory into the dropped item, so
                    // this stack is the sum of everything above plus the block entity's own state.
                    final ItemStack computerStack = new ItemStack(Items.COMPUTER.get());
                    computer(helper).exportToItemStack(computerStack);
                    assertSyncSize(registries, "computer item", computerStack, MAX_COMPUTER_STACK_BYTES);
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------- //

    private static void assertSyncSize(final RegistryAccess registries, final String what,
                                       final ItemStack stack, final int limit) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);

        final int size = buf.readableBytes();
        if (size > limit) {
            throw new GameTestAssertException(what + " syncs " + size + " bytes to clients");
        }
    }

    private static void assertBlobHandle(final String what, final ItemStack stack) {
        if (!containsKey(ItemStackUtils.getModDataTag(stack), "blob")) {
            throw new GameTestAssertException(what + " has no blob handle");
        }
    }

    private static boolean containsKey(final CompoundTag tag, final String key) {
        if (tag.contains(key)) {
            return true;
        }
        for (final String child : tag.getAllKeys()) {
            if (tag.get(child) instanceof final CompoundTag childTag && containsKey(childTag, key)) {
                return true;
            }
        }
        return false;
    }

    private static ComputerBlockEntity computer(final GameTestHelper helper) {
        return helper.getBlockEntity(COMPUTER_POS);
    }

    private static DiskDriveBlockEntity diskDrive(final GameTestHelper helper) {
        return helper.getBlockEntity(DEVICE_POS);
    }

    private static ItemHandler handler(final GameTestHelper helper, final DeviceType type) {
        return computer(helper).getItemStackHandlers().getItemHandler(type)
                .orElseThrow(() -> new GameTestAssertException("no item handler for " + type));
    }

    private static ItemStack slot(final GameTestHelper helper, final DeviceType type) {
        return handler(helper, type).getStackInSlot(0);
    }

    private static void insert(final GameTestHelper helper, final DeviceType type, final ItemStack stack) {
        if (!handler(helper, type).insertItem(0, stack, false).isEmpty()) {
            throw new GameTestAssertException("could not insert " + stack);
        }
    }

    private ItemSyncSizeTests() {
    }
}
