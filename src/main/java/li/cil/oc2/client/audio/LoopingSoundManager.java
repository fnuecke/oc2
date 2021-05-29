package li.cil.oc2.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;

import java.util.WeakHashMap;

public final class LoopingSoundManager {
    private static final WeakHashMap<TileEntity, ITickableSound> TILE_ENTITY_SOUNDS = new WeakHashMap<>();

    ///////////////////////////////////////////////////////////////////

    public static void play(final TileEntity tileEntity, final SoundEvent sound, final int delay) {
        stop(tileEntity);

        final LoopingTileEntitySound instance = new LoopingTileEntitySound(tileEntity, sound);
        TILE_ENTITY_SOUNDS.put(tileEntity, instance);
        Minecraft.getInstance().getSoundManager().playDelayed(instance, delay);
    }

    public static void stop(final TileEntity tileEntity) {
        final ITickableSound instance = TILE_ENTITY_SOUNDS.remove(tileEntity);
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }

    public static boolean isPlaying(final TileEntity tileEntity) {
        final ITickableSound instance = TILE_ENTITY_SOUNDS.get(tileEntity);
        return instance != null && !instance.isStopped();
    }
}
