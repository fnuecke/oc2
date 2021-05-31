package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundEvents {
    public static SoundEvent COMPUTER_RUNNING;
    public static SoundEvent FLOPPY_ACCESS;
    public static SoundEvent FLOPPY_EJECT;
    public static SoundEvent FLOPPY_INSERT;
    public static SoundEvent HDD_ACCESS;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        COMPUTER_RUNNING = register("computer_running");
        FLOPPY_ACCESS = register("floppy_access");
        FLOPPY_EJECT = register("floppy_eject");
        FLOPPY_INSERT = register("floppy_insert");
        HDD_ACCESS = register("hdd_access");
    }

    ///////////////////////////////////////////////////////////////////

    private static SoundEvent register(final String name) {
        ResourceLocation res = new ResourceLocation(API.MOD_ID, name);
        SoundEvent soundEvent = new SoundEvent(res);
        Registry.register(Registry.SOUND_EVENT, res, soundEvent);
        return soundEvent;
    }
}
