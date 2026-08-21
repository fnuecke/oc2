/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import io.netty.buffer.Unpooled;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.BusCables;
import li.cil.oc2.gametest.ComputerFixture;
import li.cil.oc2.gametest.DiskDriveFixture;
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

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void storageItemsSyncByHandleNotByPayload(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.EAST);
        player.setYRot(90);
        final DiskDriveFixture drive = DiskDriveFixture.place(helper, player, DEVICE_POS);

        helper.startSequence()
            .thenExecuteAfter(60, () -> {
                computer.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()))
                    .install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_CUSTOM.get()));
                drive.insert(new ItemStack(Items.FLOPPY.get()));
            })
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> {
                computer.assertRunState(VMRunState.RUNNING, "precondition");

                final RegistryAccess registries = helper.getLevel().registryAccess();

                final ItemStack hardDrive = computer.slot(DeviceTypes.HARD_DRIVE);
                final ItemStack floppy = drive.floppy();

                assertBlobHandle("hard drive", hardDrive);
                assertBlobHandle("floppy", floppy);

                assertSyncSize(registries, "flash memory",
                    computer.slot(DeviceTypes.FLASH_MEMORY), MAX_DEVICE_STACK_BYTES);
                assertSyncSize(registries, "memory",
                    computer.slot(DeviceTypes.MEMORY), MAX_DEVICE_STACK_BYTES);
                assertSyncSize(registries, "hard drive", hardDrive, MAX_DEVICE_STACK_BYTES);
                assertSyncSize(registries, "floppy", floppy, MAX_DEVICE_STACK_BYTES);

                // Breaking a computer folds its whole device inventory into the dropped item, so
                // this stack is the sum of everything above plus the block entity's own state.
                final ItemStack computerStack = new ItemStack(Items.COMPUTER.get());
                computer.blockEntity().exportToItemStack(computerStack);
                assertSyncSize(registries, "computer item", computerStack, MAX_COMPUTER_STACK_BYTES);
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

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

    // --------------------------------------------------------------------- //

    private ItemSyncSizeTests() {
    }
}
