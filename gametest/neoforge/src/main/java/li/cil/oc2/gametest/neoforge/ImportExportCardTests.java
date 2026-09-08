/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.KeyboardBlockEntity;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.util.BusCables;
import li.cil.oc2.gametest.util.Levels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ImportExportCardTests {
    private static final BlockPos KEYBOARD_POS = CABLE_POS.above();
    private static final long KEEP_ALIVE_LAPSE = 2500; /* In milliseconds, must exceed the keyboard's expiry. */

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void importExportCardSurvivesBlockEntityReload(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.place(helper);

        helper.startSequence()
            .thenExecuteAfter(40, () -> computer.install(DeviceTypes.CARD.get(),
                new ItemStack(Items.FILE_IMPORT_EXPORT_CARD.get())))
            .thenExecuteAfter(80, () -> assertCardBoundTo(computer))
            .thenExecute(() -> Levels.reloadBlockEntity(helper, computer.pos()))
            .thenExecuteAfter(120, () -> assertCardBoundTo(computer))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void keyboardUserCountsAsTerminalUser(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.UP);
        place(helper, player, new ItemStack(Items.KEYBOARD.get()), KEYBOARD_POS);

        helper.startSequence()
            .thenExecuteAfter(80, () -> {
                assertKeyboardOnBus(computer);
                if (isTerminalUser(computer, player)) {
                    throw new GameTestAssertException(
                        "a player who never touched the keyboard already counts as a terminal user");
                }
            })
            .thenExecute(() -> keyboard(helper).handleUsedBy(player))
            .thenExecute(() -> {
                if (!isTerminalUser(computer, player)) {
                    throw new GameTestAssertException(
                        "a player using the keyboard is not a terminal user of the computer, "
                            + "so the import/export card has nobody to prompt");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void keyboardUserExpiresWithoutKeepAlive(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.UP);
        place(helper, player, new ItemStack(Items.KEYBOARD.get()), KEYBOARD_POS);

        helper.startSequence()
            .thenExecuteAfter(80, () -> assertKeyboardOnBus(computer))
            .thenExecute(() -> keyboard(helper).handleUsedBy(player))
            .thenExecute(() -> {
                sleep(KEEP_ALIVE_LAPSE);
                if (isTerminalUser(computer, player)) {
                    throw new GameTestAssertException(
                        "a player who stopped sending keep-alives is still a terminal user");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void sleep(final long durationInMilliseconds) {
        try {
            Thread.sleep(durationInMilliseconds);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GameTestAssertException("interrupted while waiting: " + e);
        }
    }

    private static KeyboardBlockEntity keyboard(final GameTestHelper helper) {
        return helper.getBlockEntity(KEYBOARD_POS);
    }

    private static void assertKeyboardOnBus(final ComputerFixture computer) {
        final boolean found = computer.devices().stream().anyMatch(KeyboardDevice.class::isInstance);
        if (!found) {
            throw new GameTestAssertException("the keyboard is not on the computer's bus: " + computer.describe());
        }
    }

    private static boolean isTerminalUser(final ComputerFixture computer, final Player player) {
        for (final Player user : computer.blockEntity().getTerminalUsers()) {
            if (user == player) {
                return true;
            }
        }
        return false;
    }

    private static void assertCardBoundTo(final ComputerFixture computer) {
        final FileImportExportCardItemDevice device = computer.devices().stream()
            .filter(FileImportExportCardItemDevice.class::isInstance)
            .map(FileImportExportCardItemDevice.class::cast)
            .findFirst()
            .orElseThrow(() -> new GameTestAssertException("the import/export card is not on the bus"));

        // Only used here, so let's just grab it with reflection...
        final Object userProvider;
        try {
            final Field field = FileImportExportCardItemDevice.class.getDeclaredField("userProvider");
            field.setAccessible(true);
            userProvider = field.get(device);
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException("could not read the card's terminal user provider: " + e);
        }

        if (userProvider != computer.blockEntity()) {
            throw new GameTestAssertException(
                "the card is bound to a terminal user provider that is not the computer it sits in");
        }
    }

    // --------------------------------------------------------------------- //

    private ImportExportCardTests() {
    }
}
