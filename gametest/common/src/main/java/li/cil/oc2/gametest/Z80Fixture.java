/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Z80Fixture {
    private final ComputerFixture computer;

    // --------------------------------------------------------------------- //

    public static Z80Fixture place(final GameTestHelper helper, final Player player) {
        return new Z80Fixture(ComputerFixture.place(helper, player));
    }

    // --------------------------------------------------------------------- //

    public ComputerFixture computer() {
        return computer;
    }

    public Z80Fixture install() {
        computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_Z80.get()));
        computer.install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(FirmwareRegistry.Z80.getId()));
        computer.install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_SMALL.get()));
        return this;
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

    private Z80Fixture(final ComputerFixture computer) {
        this.computer = computer;
    }
}
