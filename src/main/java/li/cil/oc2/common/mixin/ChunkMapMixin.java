package li.cil.oc2.common.mixin;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import li.cil.oc2.common.util.ChunkAccessExt;
import li.cil.oc2.common.util.ChunkUtils;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Hooks into {@link ChunkMap} saving code-paths for "hard" save operations.
 * <p>
 * Minecraft immediately serializes all chunk data, including {@link BlockEntity} NBT.
 * This is a massive performance issue for blocks with state that changes every tick,
 * such as computers and things accepting energy.
 * <p>
 * To avoid this per-frame serialization operations, we track a "lazy unsaved" flag per
 * {@link ChunkAccess} using the {@link ChunkAccessMixin}, and flush this flag into the
 * real unsaved flag during "hard" save operations, just before the flag would be
 * checked. These save operations include:
 * <ul>
 * <li>Chunk unloaded.</li>
 * <li>Game paused (singleplayer).</li>
 * <li>Save command.</li>
 * <li>Server stopped.</li>
 * </ul>
 * <p>
 * The flag is set using the injected interface {@link ChunkAccessExt}, via the utility
 * methods in {@link ChunkUtils}.
 *
 * @see ChunkAccessMixin
 * @see ChunkUtils
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin extends ChunkStorage {
    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    public ChunkMapMixin(final Path path, final DataFixer dataFixer, final boolean sync) {
        super(path, dataFixer, sync);
    }

    /**
     * This is for the code-path taken when a chunk is being unloaded.
     */
    @Inject(method = "lambda$scheduleUnload$11", at = {@At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;save(Lnet/minecraft/world/level/chunk/ChunkAccess;)Z")})
    private void beforeSaveOnUnload(final ChunkHolder chunkHolder, final CompletableFuture<?> chunkToSave, final long chunkId, final ChunkAccess chunkAccess, final CallbackInfo ci) {
        if (chunkAccess instanceof ChunkAccessExt ext) {
            ext.applyAndClearLazyUnsaved();
        }
    }

    /**
     * This is for the code-path taken when saving all chunks upon server shutdown or when
     * running a save command with the "flush" flag.
     */
    @Inject(method = "lambda$saveAllChunks$8", at = {@At(value = "HEAD")})
    private static void beforeSyncSave(final ChunkAccess chunkAccess, final CallbackInfoReturnable<Boolean> cir) {
        if (chunkAccess instanceof ChunkAccessExt ext) {
            ext.applyAndClearLazyUnsaved();
        }
    }

    /**
     * This is for the code-path taken when saving chunk upon pausing the game or when
     * running a save command without the "flush" flag.
     */
    @Inject(method = "saveAllChunks", at = {@At(value = "HEAD")})
    private void beforeAsyncSave(final boolean sync, final CallbackInfo ci) {
        // The sync case is handled in beforeSyncSave.
        if (!sync) {
            // Need to iterate this ourselves, because I can't find the hook for the save call
            // inside the foreach in the method. Slightly annoying, but only happens on explicit
            // save requests, so not too much of a performance worry.
            visibleChunkMap.values().forEach(holder -> {
                if (holder.wasAccessibleSinceLastSave()) {
                    final ChunkAccess chunkToSave = holder.getChunkToSave().getNow(null);
                    if (chunkToSave instanceof ChunkAccessExt ext) {
                        ext.applyAndClearLazyUnsaved();
                    }
                }
            });
        }
    }
}
