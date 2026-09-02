/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.util.RegistryUtils;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.cpm.Cpm;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class FirmwareRegistry {
    private static final Registrar<BlockDeviceData> REGISTRY = RegistryUtils.builder(Registries.FIRMWARE).build();
    private static final DeferredRegister<BlockDeviceData> INITIALIZER = RegistryUtils.getInitializerFor(Registries.FIRMWARE);

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<BlockDeviceData> RISCV = INITIALIZER.register("riscv",
        () -> new FirmwareBlockDeviceData(Buildroot::getSednaFirmware, "Sedna Linux"));
    public static final RegistrySupplier<BlockDeviceData> Z80 = INITIALIZER.register("z80",
        () -> new FirmwareBlockDeviceData(Cpm::getBootRom, "CP/M 2.2"));

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }

    @Nullable
    public static ResourceLocation getKey(final BlockDeviceData data) {
        return REGISTRY.getId(data);
    }

    @Nullable
    public static BlockDeviceData getValue(final ResourceLocation location) {
        return REGISTRY.get(location);
    }

    public static Stream<BlockDeviceData> values() {
        return StreamSupport.stream(REGISTRY.spliterator(), false);
    }
}
