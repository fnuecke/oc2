/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class LevelUtils {
    @ExpectPlatform
    public static boolean fireBlockBreak(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasCorrectToolForDrops(final ServerLevel level, final ServerPlayer player, final BlockPos pos, final BlockState state) {
        throw new AssertionError();
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public static BlockEntity getBlockEntityIfChunkExists(final LevelAccessor level, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return level.getBlockEntity(pos);
    }

    @Nullable
    public static String getBlockName(final LevelAccessor level, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            final ResourceLocation registryName = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            if (registryName != null) {
                return registryName.toString();
            }
        }

        final Block block = level.getBlockState(pos).getBlock();
        {
            final ResourceLocation registryName = BuiltInRegistries.BLOCK.getKey(block);
            if (registryName != null) {
                return registryName.toString();
            }
        }

        if (blockEntity != null) {
            return blockEntity.getClass().getSimpleName();
        }

        return block.getClass().getSimpleName();
    }

    public static void playSound(final LevelAccessor level, final BlockPos pos, final SoundType soundType, final Function<SoundType, SoundEvent> soundEvent) {
        playSound(level, pos, soundType, soundEvent.apply(soundType));
    }

    public static void playSound(final LevelAccessor level, final BlockPos pos, final SoundType soundType, final SoundEvent soundEvent) {
        playSound(level, pos, soundEvent, SoundSource.BLOCKS, (soundType.getVolume() + 1f) / 2f, soundType.getPitch() * 0.8f);
    }

    public static void playSound(final LevelAccessor level, final BlockPos pos, final SoundEvent soundEvent, final SoundSource soundCategory, final float volume, final float pitch) {
        level.playSound(null, pos, soundEvent, soundCategory, volume, pitch);
    }
}
