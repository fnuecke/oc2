/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.ConnectorFixture;
import li.cil.oc2.gametest.fixture.HubFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class SerialPortTests {
    private static final String BATCH = "oc2_serial_port";
    private static final BlockPos RELAY = COMPUTER_POS.east(4).south(2);

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void serialCardsCarryBytesOverTheLine(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        final ItemStack upOnly = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        final ItemStack allButUp = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        for (final Direction side : Direction.values()) {
            SerialInterfaceCardItem.setSideConfiguration(upOnly, side, side == Direction.UP);
        }
        SerialInterfaceCardItem.setSideConfiguration(allButUp, Direction.UP, false);
        SerialInterfaceCardItem.setAddress(upOnly, 3);
        SerialInterfaceCardItem.setAddress(allButUp, 7);

        final ConnectorFixture[] connectors = new ConnectorFixture[3];

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                computer
                    .install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
                    .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                    .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()))
                    .install(DeviceTypes.CARD.get(), upOnly)
                    .install(DeviceTypes.CARD.get(), allButUp);

                HubFixture.place(helper, player, RELAY);

                player.setXRot(90);
                connectors[0] = ConnectorFixture.place(helper, player, computer.pos().above());
                connectors[2] = ConnectorFixture.place(helper, player, RELAY.above());
                player.setXRot(0);
                player.setYRot(90);
                connectors[1] = ConnectorFixture.place(helper, player, computer.pos().east());
                connectors[0].linkTo(connectors[2]);
                connectors[1].linkTo(connectors[2]);
            })
            .thenExecuteAfter(40, computer::start)
            .thenWaitUntil(() -> computer.assertScreenContains("login:", "the guest should reach its login prompt"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("#", "root should get a shell"))
            .thenExecute(() -> {
                final Direction[] sides = {Direction.UP, Direction.EAST};
                for (int i = 0; i < sides.length; i++) {
                    final Object resolved = connectors[i].adjacentInterface();
                    if (resolved == null || resolved != computer.networkInterface(sides[i])) {
                        throw new GameTestAssertException("the connector on " + sides[i] + " resolved " + resolved
                            + ", the computer offers " + computer.networkInterface(sides[i]));
                    }
                }
            })
            .thenExecute(() -> computer.type(script(
                "stty -F /dev/ttyS1 9600 raw -echo",
                "stty -F /dev/ttyS2 9600 raw -echo",
                "timeout 5 head -c 2 /dev/ttyS2 > /tmp/rx &",
                "sleep 1",
                "printf hi > /dev/ttyS1",
                "wait",
                "echo RX=$(cat /tmp/rx) END")))
            .thenWaitUntil(() -> computer.assertScreenContains("RX=hi END",
                "bytes written to one port should reach the card on the other end"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void cardsSharingASideDoNotShadowEachOther(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        final ItemStack[] cards = {
            upOnlyCard(3),
            upOnlyCard(7),
            allButUpCard(11),
        };

        final ConnectorFixture[] connectors = new ConnectorFixture[3];

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                computer
                    .install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
                    .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                    .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()))
                    .install(DeviceTypes.CARD.get(), new ItemStack(Items.NETWORK_INTERFACE_CARD.get()))
                    .install(DeviceTypes.CARD.get(), cards[0])
                    .install(DeviceTypes.CARD.get(), cards[1])
                    .install(DeviceTypes.CARD.get(), cards[2]);

                HubFixture.place(helper, player, RELAY);

                player.setXRot(90);
                connectors[0] = ConnectorFixture.place(helper, player, computer.pos().above());
                connectors[2] = ConnectorFixture.place(helper, player, RELAY.above());
                player.setXRot(0);
                player.setYRot(90);
                connectors[1] = ConnectorFixture.place(helper, player, computer.pos().east());
                connectors[0].linkTo(connectors[2]);
                connectors[1].linkTo(connectors[2]);
            })
            .thenExecuteAfter(40, computer::start)
            .thenWaitUntil(() -> computer.assertScreenContains("login:", "the guest should reach its login prompt"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("#", "root should get a shell"))
            .thenExecute(() -> computer.type(script(
                "for p in 1 2 3; do stty -F /dev/ttyS$p 9600 raw -echo; done",
                "timeout 5 head -c 2 /dev/ttyS1 > /tmp/a &",
                "timeout 5 head -c 2 /dev/ttyS2 > /tmp/b &",
                "sleep 1",
                "printf hi > /dev/ttyS3",
                "wait",
                "echo RX=$(cat /tmp/a)/$(cat /tmp/b) END")))
            .thenWaitUntil(() -> computer.assertScreenContains("RX=hi/hi END",
                "every card on the segment should hear the sender"))
            .thenExecute(() -> computer.type(script(
                "timeout 5 head -c 2 /dev/ttyS3 > /tmp/c &",
                "sleep 1",
                "printf a > /dev/ttyS1",
                "sleep 1",
                "printf b > /dev/ttyS2",
                "wait",
                "echo TX=$(fold -w1 /tmp/c | sort | tr -d '\\n') END")))
            .thenWaitUntil(() -> computer.assertScreenContains("TX=ab END",
                "both cards sharing a side should reach the peer"))
            .thenExecute(() -> computer.type(script(
                "timeout 5 head -c 1 /dev/ttyS2 > /tmp/d &",
                "sleep 1",
                "printf c > /dev/ttyS1",
                "wait",
                "echo SIDE=$(cat /tmp/d) END")))
            .thenWaitUntil(() -> computer.assertScreenContains("SIDE=c END",
                "cards sharing a side should hear each other"))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ItemStack upOnlyCard(final int address) {
        final ItemStack stack = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        for (final Direction side : Direction.values()) {
            SerialInterfaceCardItem.setSideConfiguration(stack, side, side == Direction.UP);
        }
        SerialInterfaceCardItem.setAddress(stack, address);
        return stack;
    }

    private static ItemStack allButUpCard(final int address) {
        final ItemStack stack = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        SerialInterfaceCardItem.setSideConfiguration(stack, Direction.UP, false);
        SerialInterfaceCardItem.setAddress(stack, address);
        return stack;
    }

    // --------------------------------------------------------------------- //

    private SerialPortTests() {
    }
}
