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
    public static void linuxSeesTheSerialCardAsASerialPort(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer
                .install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
                .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()))
                .install(DeviceTypes.CARD.get(), new ItemStack(Items.SERIAL_INTERFACE_CARD.get())))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> computer.assertScreenContains("login:", "the guest should reach its login prompt"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("#", "root should get a shell"))
            .thenExecute(() -> computer.type("stty -F /dev/ttyS1 -a | head -1\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("speed",
                "the card should come up as a serial port the guest can configure"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void theLibrariesTalkOverARealLine(final GameTestHelper helper) {
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

                HubFixture.place(helper, player, RELAY); // any solid block to hang the relay connector on

                player.setXRot(90);
                connectors[0] = ConnectorFixture.place(helper, player, computer.pos().above());
                connectors[2] = ConnectorFixture.place(helper, player, RELAY.above());
                player.setXRot(0);
                player.setYRot(90); // looking west, at the computer's side; its front face cannot hold a connector
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
            .thenExecute(() -> computer.type("micropython -c \"from oc2 import serial as s;a=s.open('/dev/ttyS1',9600);" +
                "b=s.open('/dev/ttyS2',9600);a.broadcast(b'hi');r=b.receive(5000);" +
                "print('RX'+'-OK',r[1],r[0]==a.address,sorted((a.address,b.address))==[3,7])\"\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("RX-OK b'hi' True True",
                "a short frame should come back from receive, stamped with the address of the card behind the sending port"))
            .thenExecute(() -> computer.type("lua -e \"local l=assert(require('oc2.serial').open('/dev/ttyS1'));" +
                "print('CO'..'LL='..tostring(l:collisions()))\"\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("COLL=0",
                "collisions() should read the card's transmit error counter"))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private SerialPortTests() {
    }
}
