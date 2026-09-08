/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fixture;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.util.BusCables;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc2.gametest.util.TestSupport.CABLE_POS;
import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;

public final class Z80Fixture {
    private final ComputerFixture computer;
    private final DiskDriveFixture drive;

    // --------------------------------------------------------------------- //

    public static Z80Fixture place(final GameTestHelper helper, final Player player) {
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.EAST);
        player.setYRot(90);
        return new Z80Fixture(computer, DiskDriveFixture.place(helper, player, DEVICE_POS));
    }

    // --------------------------------------------------------------------- //

    public ComputerFixture computer() {
        return computer;
    }

    public Z80Fixture install() {
        computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_Z80.get()));
        computer.install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_Z80.getId()));
        computer.install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_SMALL.get()));
        drive.insert(Items.FLOPPY.get().withData(BlockDeviceDataRegistry.CPM.getId()));
        return this;
    }

    public DiskDriveFixture drive() {
        return drive;
    }

    public Z80Fixture withRedstoneCard() {
        computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.REDSTONE_INTERFACE_CARD.get()));
        return this;
    }

    public void start() {
        computer.start();
    }

    public String screen() {
        return computer.screen();
    }

    public void command(final String text) {
        computer.type(text + "\r");
    }

    public void assertScreenContains(final String expected, final String what) {
        computer.assertScreenContains(expected, what);
    }

    // --------------------------------------------------------------------- //

    private Z80Fixture(final ComputerFixture computer, final DiskDriveFixture drive) {
        this.computer = computer;
        this.drive = drive;
    }
}
