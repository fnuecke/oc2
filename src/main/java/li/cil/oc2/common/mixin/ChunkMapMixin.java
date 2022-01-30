package li.cil.oc2.common.mixin;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import li.cil.oc2.common.ext.ChunkAccessExt;
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

import java.nio.file.Path;

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

    @Inject(method = "saveAllChunks", at = {@At(value = "HEAD")})
    private void beforeAsyncSave(final CallbackInfo ci) {
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
