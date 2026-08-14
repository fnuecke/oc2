/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComputerTests {
    @GameTest(template = TEMPLATE)
    public static void deviceTypesAreRegistered(final GameTestHelper helper) {
        assertNotNull(helper, DeviceTypes.MEMORY, "DeviceTypes.MEMORY");
        assertNotNull(helper, DeviceTypes.HARD_DRIVE, "DeviceTypes.HARD_DRIVE");
        assertNotNull(helper, DeviceTypes.FLASH_MEMORY, "DeviceTypes.FLASH_MEMORY");
        assertNotNull(helper, DeviceTypes.CARD, "DeviceTypes.CARD");
        assertNotNull(helper, DeviceTypes.ROBOT_MODULE, "DeviceTypes.ROBOT_MODULE");
        assertNotNull(helper, DeviceTypes.FLOPPY, "DeviceTypes.FLOPPY");
        assertNotNull(helper, DeviceTypes.NETWORK_TUNNEL, "DeviceTypes.NETWORK_TUNNEL");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void computerInitializesCleanly(final GameTestHelper helper) {
        placeComputer(helper);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
                    if (computer.getVirtualMachine().getRunState() != VMRunState.STOPPED) {
                        throw new GameTestAssertException("computer should idle in STOPPED, was "
                                + computer.getVirtualMachine().getRunState());
                    }
                    if (computer.getVirtualMachine().getBootError() != null) {
                        throw new GameTestAssertException("expected no boot error on a fresh computer");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void updateTagRoundTripsWithoutError(final GameTestHelper helper) {
        placeComputer(helper);
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);

                    final var registries = helper.getLevel().registryAccess();
                    final CompoundTag tag = computer.getUpdateTag(registries);
                    computer.loadWithComponents(tag, registries);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void breakingComputerKeepsContents(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        placeComputer(helper);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
                    final ItemHandler memory = computer.getItemStackHandlers()
                            .getItemHandler(DeviceTypes.MEMORY)
                            .orElseThrow(() -> new GameTestAssertException("no memory item handler"));
                    final ItemStack leftover = memory.insertItem(0,
                            new ItemStack(Items.MEMORY_SMALL.get()), false);
                    if (!leftover.isEmpty()) {
                        throw new GameTestAssertException("could not insert memory into the computer");
                    }
                })
                .thenExecuteAfter(10, () -> helper.getLevel()
                        .destroyBlock(helper.absolutePos(COMPUTER_POS), true, null))
                .thenExecuteAfter(10, () -> {
                    final ItemStack dropped = singleDroppedStack(helper);
                    if (!dropped.is(Items.COMPUTER.get())) {
                        throw new GameTestAssertException("expected a computer item, got " + dropped);
                    }

                    final CompoundTag data = ItemStackUtils.getBlockEntityDataTag(dropped);
                    if (data.isEmpty()) {
                        throw new GameTestAssertException(
                                "dropped computer carries no block entity data — contents were lost");
                    }
                    if (!data.contains("items")) {
                        throw new GameTestAssertException("dropped computer carries no items tag");
                    }
                    if (!data.contains("energy")) {
                        throw new GameTestAssertException("dropped computer carries no energy tag");
                    }

                    helper.killAllEntities();

                    place(helper, player, dropped, COMPUTER_POS);
                })
                .thenExecuteAfter(20, () -> {
                    final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
                    final ItemHandler memory = computer.getItemStackHandlers()
                            .getItemHandler(DeviceTypes.MEMORY)
                            .orElseThrow(() -> new GameTestAssertException("no memory item handler"));
                    helper.assertFalse(memory.extractItem(0, 1, false).isEmpty(),
                            "memory item was not restored after re-placing computer");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void normalInteractionDoesNotStart(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        placeComputer(helper);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.useBlock(COMPUTER_POS, player);

                    final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
                    if (computer.getVirtualMachine().getRunState() != VMRunState.STOPPED) {
                        throw new GameTestAssertException(
                                "a non-sneaking interaction must not start the computer");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void sneakInteractStarts(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        placeComputer(helper);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    player.setShiftKeyDown(true);
                    helper.useBlock(COMPUTER_POS, player);

                    final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
                    if (computer.getVirtualMachine().getRunState() == VMRunState.STOPPED) {
                        throw new GameTestAssertException(
                                "a sneaking interaction should have started the computer");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void importExportCardSurvivesBlockEntityReload(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final ItemStack leftover = ((ComputerBlockEntity) helper.getBlockEntity(COMPUTER_POS))
                            .getItemStackHandlers()
                            .getItemHandler(DeviceTypes.CARD)
                            .orElseThrow(() -> new GameTestAssertException("no card slot"))
                            .insertItem(0, new ItemStack(Items.FILE_IMPORT_EXPORT_CARD.get()), false);
                    if (!leftover.isEmpty()) {
                        throw new GameTestAssertException("could not install the import/export card");
                    }
                })
                .thenExecuteAfter(80, () -> assertImportExportDeviceBoundToComputer(helper))
                .thenExecute(() -> reloadBlockEntity(helper, COMPUTER_POS))
                .thenExecuteAfter(120, () -> assertImportExportDeviceBoundToComputer(helper))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void breakingDiskDriveDropsItsFloppy(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.DISK_DRIVE.get()), COMPUTER_POS);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    final DiskDriveBlockEntity drive = helper.getBlockEntity(COMPUTER_POS);
                    if (!drive.insert(new ItemStack(Items.FLOPPY.get()), null).isEmpty()) {
                        throw new GameTestAssertException("could not insert the floppy");
                    }
                })
                .thenExecuteAfter(10, () -> helper.getLevel()
                        .destroyBlock(helper.absolutePos(COMPUTER_POS), true, null))
                .thenExecuteAfter(10, () -> {
                    final List<ItemStack> dropped = droppedStacks(helper);
                    if (dropped.stream().noneMatch(stack -> stack.is(Items.FLOPPY.get()))) {
                        throw new GameTestAssertException("the floppy was destroyed with the drive, got " + dropped);
                    }
                    if (dropped.stream().noneMatch(stack -> stack.is(Items.DISK_DRIVE.get()))) {
                        throw new GameTestAssertException("the drive itself did not drop, got " + dropped);
                    }
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------- //

    private static List<ItemStack> droppedStacks(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2))
                .stream().map(ItemEntity::getItem).toList();
    }

    private static ItemStack singleDroppedStack(final GameTestHelper helper) {
        final List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                helper.getBounds().inflate(2));
        if (items.size() != 1) {
            throw new GameTestAssertException("expected exactly one dropped stack, found " + items.size());
        }
        return items.getFirst().getItem();
    }

    private static void placeComputer(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);
    }

    private static void assertImportExportDeviceBoundToComputer(final GameTestHelper helper) {
        final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
        final FileImportExportCardItemDevice device =
                ((AbstractVirtualMachine) computer.getVirtualMachine()).busController.getDevices().stream()
                        .filter(FileImportExportCardItemDevice.class::isInstance)
                        .map(FileImportExportCardItemDevice.class::cast)
                        .findFirst()
                        .orElseThrow(() -> new GameTestAssertException("the import/export card is not on the bus"));

        // Only used here, so let's just grab it with reflection...
        final Object userProvider;
        try {
            final java.lang.reflect.Field field =
                    FileImportExportCardItemDevice.class.getDeclaredField("userProvider");
            field.setAccessible(true);
            userProvider = field.get(device);
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException("could not read the card's terminal user provider: " + e);
        }

        if (userProvider != computer) {
            throw new GameTestAssertException(
                    "the card is bound to a terminal user provider that is not the computer it sits in");
        }
    }

    /**
     * As close to what a world reload does to a single block entity as we can get in a game test: save it,
     * remove it from the level, then bring a fresh instance back from that data.
     */
    private static void reloadBlockEntity(final GameTestHelper helper, final BlockPos relativePos) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(relativePos);
        final BlockState state = level.getBlockState(pos);

        final BlockEntity previous = level.getBlockEntity(pos);
        if (previous == null) {
            throw new GameTestAssertException("nothing to reload at " + relativePos);
        }

        final CompoundTag tag = previous.saveWithFullMetadata(level.registryAccess());
        level.removeBlockEntity(pos);

        final BlockEntity restored = BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
        if (restored == null) {
            throw new GameTestAssertException("could not restore the block entity from its own data");
        }
        level.setBlockEntity(restored);
    }

    private ComputerTests() {
    }
}
