/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.ComputerFixture;
import li.cil.oc2.gametest.Drops;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComputerTests {
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void computerInitializesCleanly(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                computer.assertRunState(VMRunState.STOPPED, "a fresh computer should idle");
                computer.assertNoBootError();
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void updateTagRoundTripsWithoutError(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        helper.startSequence()
            .thenExecuteAfter(10, () -> computer.load(computer.updateTag()))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void breakingComputerKeepsContents(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        helper.startSequence()
            .thenExecuteAfter(20, () -> computer.install(DeviceTypes.MEMORY,
                new ItemStack(Items.MEMORY_SMALL.get())))
            .thenExecuteAfter(10, () -> breakBlockAndDrop(helper, computer.pos()))
            .thenExecuteAfter(10, () -> {
                final ItemStack dropped = Drops.single(helper);
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

                place(helper, player, dropped, computer.pos());
            })
            .thenExecuteAfter(20, () -> helper.assertFalse(
                computer.handler(DeviceTypes.MEMORY).extractItem(0, 1, false).isEmpty(),
                "memory item was not restored after re-placing computer"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void normalInteractionDoesNotStart(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                helper.useBlock(computer.pos(), player);
                computer.assertRunState(VMRunState.STOPPED,
                    "a non-sneaking interaction must not start the computer");
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void sneakInteractStarts(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                player.setShiftKeyDown(true);
                helper.useBlock(computer.pos(), player);

                if (computer.runState() == VMRunState.STOPPED) {
                    throw new GameTestAssertException(
                        "a sneaking interaction should have started the computer");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private ComputerTests() {
    }
}
