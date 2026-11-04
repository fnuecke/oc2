/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fixture;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import net.minecraft.world.item.ItemStack;

public final class Hardware {
    public static void installLinux(final MachineFixture machine) {
        machine.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
            .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
            .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
            .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.SEDNA_LINUX.getId()));
    }

    public static void installOpenOs2(final MachineFixture machine) {
        machine.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
            .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
            .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_SMALL.get()))
            .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_SMALL.get().withData(BlockDeviceDataRegistry.OPEN_OS2.getId()));
    }

    public static void installLinuxWithExtraMemory(final MachineFixture machine) {
        installLinux(machine);
        machine.install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()));
    }

    // --------------------------------------------------------------------- //

    private Hardware() {
    }
}
