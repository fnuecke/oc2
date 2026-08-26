/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.ComputerFixture;
import li.cil.oc2.gametest.Levels;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ImportExportCardTests {
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

    // --------------------------------------------------------------------- //

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
