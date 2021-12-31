package li.cil.oc2.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.WeakHashMap;

public final class LoopingSoundManager {
    private static final WeakHashMap<BlockEntity, TickableSoundInstance> BLOCK_ENTITY_SOUNDS = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void play(final BlockEntity blockEntity, final SoundEvent sound, final int delay) {
        stop(blockEntity);

        final LoopingBlockEntitySound instance = new LoopingBlockEntitySound(blockEntity, sound);
        BLOCK_ENTITY_SOUNDS.put(blockEntity, instance);
        Minecraft.getInstance().getSoundManager().playDelayed(instance, delay);
    }

    public static void stop(final BlockEntity blockEntity) {
        final TickableSoundInstance instance = BLOCK_ENTITY_SOUNDS.remove(blockEntity);
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public static boolean isPlaying(final BlockEntity blockEntity) {
        final TickableSoundInstance instance = BLOCK_ENTITY_SOUNDS.get(blockEntity);
        return instance != null && !instance.isStopped();
    }
}
