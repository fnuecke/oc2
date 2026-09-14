/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.common.vm.CpmSystemDisk;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.cpm.Cpm;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public final class BuiltinBlockDeviceDataTests {
    @Test
    public void capacityDoesNotOpenTheBlockDevice() {
        final BuiltinBlockDeviceData data = new BuiltinBlockDeviceData(throwingBlockDevice(), () -> 42L, "Test", DyeColor.WHITE);

        assertEquals(42L, data.getCapacity());
    }

    @Test
    public void blockDeviceIsOpenedLazily() {
        assertDoesNotThrow(() -> new BuiltinBlockDeviceData(throwingBlockDevice(), "Test", DyeColor.WHITE));
    }

    @Test
    public void cpmFloppyReportsItsCapacityBeforeComposition() {
        final BuiltinBlockDeviceData data = new BuiltinBlockDeviceData(
            BuiltinBlockDeviceData.readEachTime(CpmSystemDisk::getImage),
            Cpm.DiskGeometry::getImageSize, "CP/M 2.2", DyeColor.ORANGE);

        assertEquals(Cpm.DiskGeometry.getImageSize(), data.getCapacity());
        assertThrows(IllegalStateException.class, data::getBlockDevice,
            "using the contents before composition must still fail loudly");
    }

    // --------------------------------------------------------------------- //

    private static Supplier<BlockDevice> throwingBlockDevice() {
        return () -> {
            throw new IllegalStateException("the block device must not be opened to answer the capacity");
        };
    }
}
