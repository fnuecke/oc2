/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.util.Drops;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.*;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "computer")
public final class ComputerTests {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 200)
    @TestHolder
    public static void updateTagRoundTripsWithoutError(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);
        helper.startSequence()
            .thenExecuteAfter(10, () -> computer.load(computer.updateTag()))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void breakingComputerKeepsContents(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        helper.startSequence()
            .thenExecuteAfter(20, () -> computer.install(DeviceTypes.MEMORY.get(),
                new ItemStack(Items.MEMORY_SMALL.get())))
            .thenExecuteAfter(10, () -> breakBlockAndDrop(helper, computer.pos()))
            .thenExecuteAfter(10, () -> {
                final ItemStack dropped = Drops.single(helper);
                if (!dropped.is(Items.COMPUTER.get())) {
                    throw new GameTestAssertException(Component.literal("expected a computer item, got " + dropped), 0);
                }

                helper.killAllEntities();

                place(helper, player, dropped, computer.pos());
            })
            .thenExecuteAfter(20, () -> helper.assertFalse(
                Transaction.run(transaction -> computer.handler(DeviceTypes.MEMORY.get()).extractItem(0, 1, transaction)).isEmpty(),
                "memory item was not restored after re-placing computer"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
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

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 300)
    @TestHolder
    public static void sneakInteractStarts(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                player.setShiftKeyDown(true);
                helper.useBlock(computer.pos(), player);

                if (computer.runState() == VMRunState.STOPPED) {
                    throw new GameTestAssertException(Component.literal("sneak interaction should start the computer"), 0);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private ComputerTests() {
    }
}
