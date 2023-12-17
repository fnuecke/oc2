/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.WeakHashMap;

public final class LoopingSoundManager {
    private static final WeakHashMap<BlockEntity, LoopingBlockEntitySound> BLOCK_ENTITY_SOUNDS = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void play(final BlockEntity blockEntity, final SoundEvent sound, final int delay, final RandomSource random) {
        stop(blockEntity);

        final LoopingBlockEntitySound instance = new LoopingBlockEntitySound(blockEntity, sound, random);
        BLOCK_ENTITY_SOUNDS.put(blockEntity, instance);
        Minecraft.getInstance().getSoundManager().playDelayed(instance, delay);
    }

    public static void stop(final BlockEntity blockEntity) {
        final LoopingBlockEntitySound instance = BLOCK_ENTITY_SOUNDS.remove(blockEntity);
        if (instance != null) {
            instance.cancel();
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public static boolean isPlaying(final BlockEntity blockEntity) {
        final LoopingBlockEntitySound instance = BLOCK_ENTITY_SOUNDS.get(blockEntity);
        return instance != null && !instance.isStopped();
    }
}
