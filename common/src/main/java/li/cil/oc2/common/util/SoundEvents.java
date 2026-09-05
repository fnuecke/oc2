/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.API;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundEvents {
    private static final DeferredRegister<SoundEvent> SOUNDS = RegistryUtils.getInitializerFor(Registries.SOUND_EVENT);

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<SoundEvent> COMPUTER_RUNNING = register("computer_running");
    public static final RegistrySupplier<SoundEvent> FLOPPY_ACCESS = register("floppy_access");
    public static final RegistrySupplier<SoundEvent> FLOPPY_EJECT = register("floppy_eject");
    public static final RegistrySupplier<SoundEvent> FLOPPY_INSERT = register("floppy_insert");
    public static final RegistrySupplier<SoundEvent> FLASH_EJECT = register("flash_eject");
    public static final RegistrySupplier<SoundEvent> FLASH_INSERT = register("flash_insert");
    public static final RegistrySupplier<SoundEvent> HDD_ACCESS = register("hdd_access");

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }

    // --------------------------------------------------------------------- //

    private static RegistrySupplier<SoundEvent> register(final String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, name)));
    }
}
