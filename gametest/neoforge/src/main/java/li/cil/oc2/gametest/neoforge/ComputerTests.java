/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
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

    // ------------------------------------------------------------- //

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

    private ComputerTests() {
    }
}
