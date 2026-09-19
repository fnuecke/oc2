/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.ConnectorFixture;
import li.cil.oc2.gametest.util.TestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class TerminalTests {
    private static final String BATCH = "oc2_terminal";

    private static final int TERMINAL_ADDRESS = 5;
    private static final int CARD_ADDRESS = 7;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void terminalAndAGuestTalkOverTheWire(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        final ItemStack card = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        SerialInterfaceCardItem.setSideConfiguration(card, Direction.UP, false);
        SerialInterfaceCardItem.setAddress(card, CARD_ADDRESS);

        final BlockPos terminalPos = computer.pos().east(5);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                computer
                    .install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
                    .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                    .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()))
                    .install(DeviceTypes.CARD.get(), card);

                // The screen looks east, so the wire goes on its west face.
                placeTerminal(helper, player, terminalPos, Direction.EAST);

                player.setYRot(90); // looking west, so the connector hangs on the computer's east side
                final ConnectorFixture atComputer = ConnectorFixture.place(helper, player, computer.pos().east());
                final ConnectorFixture atTerminal = placeConnectorOn(helper, player, terminalPos, Direction.WEST);
                atComputer.linkTo(atTerminal);

                terminal(helper, terminalPos).setConfiguration(TERMINAL_ADDRESS, 9600);
            })
            .thenExecuteAfter(40, () -> {
                if (ConnectorFixture.at(helper, terminalPos.west()).adjacentInterface() == null) {
                    throw new GameTestAssertException("the connector did not find the terminal");
                }
                if (ConnectorFixture.at(helper, computer.pos().east()).adjacentInterface() == null) {
                    throw new GameTestAssertException("the connector did not find the computer's card");
                }
                computer.start();
            })
            .thenWaitUntil(() -> computer.assertScreenContains("login:", "the guest should reach its login prompt"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> computer.assertScreenContains("#", "root should get a shell"))

            .thenExecute(() -> computer.type("stty -F /dev/ttyS1 9600 raw -echo; exec 3<> /dev/ttyS1\n"))
            .thenExecuteAfter(20, () -> computer.type("echo pong >&3\n"))
            .thenWaitUntil(() -> assertTerminalContains(helper, terminalPos, "pong",
                "what the guest writes should print on the terminal"))
            .thenExecute(() -> computer.type("cat -v <&3 &\n"))
            .thenExecuteAfter(40, () -> terminal(helper, terminalPos).getTerminal()
                .putInput(ByteBuffer.wrap("ping\n".getBytes(StandardCharsets.US_ASCII))))
            .thenWaitUntil(() -> computer.assertScreenContains("ping",
                "and what the terminal types should reach the guest"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void twoTerminalsTalkToEachOther(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);

        final BlockPos firstPos = COMPUTER_POS.east(2);
        final BlockPos secondPos = COMPUTER_POS.east(6);

        helper.startSequence()
            .thenExecute(() -> {
                linkTerminals(helper, player, firstPos, secondPos);

                terminal(helper, firstPos).setConfiguration(3, 9600);
                terminal(helper, secondPos).setConfiguration(4, 9600);
            })
            .thenExecuteAfter(20, () -> terminal(helper, firstPos).getTerminal()
                .putInput(ByteBuffer.wrap("hello".getBytes(StandardCharsets.US_ASCII))))
            .thenExecuteAfter(20, () -> assertTerminalContains(helper, secondPos, "hello",
                "what one terminal types should print on the other"))
            .thenExecute(() -> assertTerminalLacks(helper, firstPos, "hello",
                "and not on the one that typed it, there is no local echo"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void terminalIsConnectedWhileAConnectorPollsIt(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);

        final BlockPos firstPos = COMPUTER_POS.east(2);
        final BlockPos secondPos = COMPUTER_POS.east(6);
        final BlockPos lonePos = COMPUTER_POS.east(4);

        helper.startSequence()
            .thenExecute(() -> {
                linkTerminals(helper, player, firstPos, secondPos);
                placeTerminal(helper, player, lonePos, Direction.NORTH);
            })
            .thenExecuteAfter(20, () -> {
                assertConnected(helper, firstPos, true, "a terminal with a connector at its back is connected");
                assertConnected(helper, lonePos, false, "a terminal without one is not");
                helper.destroyBlock(firstPos.east());
            })
            .thenExecuteAfter(20, () -> assertConnected(helper, firstPos, false,
                "a terminal whose connector was removed is no longer connected"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void terminalAtAnotherRateHearsNoise(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);

        final BlockPos firstPos = COMPUTER_POS.east(2);
        final BlockPos secondPos = COMPUTER_POS.east(6);

        helper.startSequence()
            .thenExecute(() -> {
                linkTerminals(helper, player, firstPos, secondPos);
                terminal(helper, firstPos).setConfiguration(3, 9600);
                terminal(helper, secondPos).setConfiguration(4, 1200);
            })
            .thenExecuteAfter(20, () -> terminal(helper, firstPos).getTerminal()
                .putInput(ByteBuffer.wrap("hello".getBytes(StandardCharsets.US_ASCII))))
            .thenExecuteAfter(20, () -> assertTerminalContains(helper, secondPos, "~",
                "a terminal set to another rate hears noise"))
            .thenExecute(() -> assertTerminalLacks(helper, secondPos, "hello",
                "and not the data"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = BATCH)
    public static void terminalsSharingAnAddressDoNotHearEachOther(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);

        final BlockPos firstPos = COMPUTER_POS.east(2);
        final BlockPos secondPos = COMPUTER_POS.east(6);

        helper.startSequence()
            .thenExecute(() -> {
                linkTerminals(helper, player, firstPos, secondPos);
                terminal(helper, firstPos).setConfiguration(3, 9600);
                terminal(helper, secondPos).setConfiguration(3, 9600);
            })
            .thenExecuteAfter(20, () -> terminal(helper, firstPos).getTerminal()
                .putInput(ByteBuffer.wrap("hello".getBytes(StandardCharsets.US_ASCII))))
            .thenExecuteAfter(20, () -> assertTerminalLacks(helper, secondPos, "hello",
                "an endpoint ignores what carries its own address"))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void linkTerminals(final GameTestHelper helper, final Player player, final BlockPos firstPos, final BlockPos secondPos) {
        placeTerminal(helper, player, firstPos, Direction.WEST);
        placeTerminal(helper, player, secondPos, Direction.EAST);

        final ConnectorFixture atFirst = placeConnectorOn(helper, player, firstPos, Direction.EAST);
        final ConnectorFixture atSecond = placeConnectorOn(helper, player, secondPos, Direction.WEST);
        atFirst.linkTo(atSecond);
    }

    private static void placeTerminal(final GameTestHelper helper, final Player player, final BlockPos pos, final Direction facing) {
        player.setYRot(facing.getOpposite().toYRot());
        TestSupport.place(helper, player, new ItemStack(Items.TERMINAL.get()), pos);
        if (!(helper.getBlockEntity(pos) instanceof TerminalBlockEntity)) {
            throw new GameTestAssertException("no terminal at " + pos);
        }
    }

    private static ConnectorFixture placeConnectorOn(final GameTestHelper helper, final Player player, final BlockPos pos, final Direction face) {
        final BlockPos connectorPos = pos.relative(face);
        TestSupport.useOn(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), pos, face);
        if (!helper.getBlockState(connectorPos).is(li.cil.oc2.common.block.Blocks.NETWORK_CONNECTOR.get())) {
            throw new GameTestAssertException("no connector at " + connectorPos);
        }
        return ConnectorFixture.at(helper, connectorPos);
    }

    private static void assertConnected(final GameTestHelper helper, final BlockPos pos, final boolean expected, final String what) {
        if (terminal(helper, pos).isConnected() != expected) {
            throw new GameTestAssertException(what);
        }
    }

    private static TerminalBlockEntity terminal(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    private static void assertTerminalContains(final GameTestHelper helper, final BlockPos pos, final String expected, final String what) {
        final String text = screenOf(helper, pos);
        if (!text.contains(expected)) {
            throw new GameTestAssertException(what + ": terminal does not hold [" + expected + "]\n" + text);
        }
    }

    private static void assertTerminalLacks(final GameTestHelper helper, final BlockPos pos, final String unexpected, final String what) {
        final String text = screenOf(helper, pos);
        if (text.contains(unexpected)) {
            throw new GameTestAssertException(what + ": terminal holds [" + unexpected + "]\n" + text);
        }
    }

    private static String screenOf(final GameTestHelper helper, final BlockPos pos) {
        final Terminal value = terminal(helper, pos).getTerminal();
        final CompoundTag tag;
        synchronized (value) {
            tag = NBTSerialization.serialize(value);
        }

        final byte[] buffer = tag.getByteArray("buffer");
        final StringBuilder text = new StringBuilder();
        for (int row = 0; row < Terminal.HEIGHT; row++) {
            for (int column = 0; column < Terminal.WIDTH; column++) {
                final int index = row * Terminal.WIDTH + column;
                final byte character = index < buffer.length ? buffer[index] : 0;
                text.append(character == 0 ? ' ' : (char) (character & 0xFF));
            }
            text.append('\n');
        }

        return text.toString();
    }

    // --------------------------------------------------------------------- //

    private TerminalTests() {
    }
}
