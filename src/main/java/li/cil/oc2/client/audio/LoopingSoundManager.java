package li.cil.oc2.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.WeakHashMap;

public final class LoopingSoundManager {
    private static final WeakHashMap<BlockEntity, TickableSoundInstance> TILE_ENTITY_SOUNDS = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void play(final BlockEntity tileEntity, final SoundEvent sound, final int delay) {
        stop(tileEntity);

        final LoopingBlockEntitySound instance = new LoopingBlockEntitySound(tileEntity, sound);
        TILE_ENTITY_SOUNDS.put(tileEntity, instance);
        Minecraft.getInstance().getSoundManager().playDelayed(instance, delay);
    }

    public static void stop(final BlockEntity tileEntity) {
        final TickableSoundInstance instance = TILE_ENTITY_SOUNDS.remove(tileEntity);
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public static boolean isPlaying(final BlockEntity tileEntity) {
        final TickableSoundInstance instance = TILE_ENTITY_SOUNDS.get(tileEntity);
        return instance != null && !instance.isStopped();
    }
}
