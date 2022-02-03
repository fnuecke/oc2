/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MessageUtils {
    public static <T extends BlockEntity> void withNearbyServerBlockEntityForInteraction(final NetworkEvent.Context context, final BlockPos pos, final Class<T> type, final BiConsumer<ServerPlayer, T> callback) {
        final ServerPlayer player = context.getSender();
        if (player == null || !pos.closerThan(player.position(), 8)) {
            return;
        }

        withNearbyServerBlockEntity(context, pos, type, callback);
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withNearbyServerBlockEntity(final NetworkEvent.Context context, final BlockPos pos, final Class<T> type, final BiConsumer<ServerPlayer, T> callback) {
        final ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        final ServerLevel level = player.getLevel();
        final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, pos);
        if (type.isInstance(blockEntity)) {
            callback.accept(player, (T) blockEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withServerEntity(final NetworkEvent.Context context, final int id, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        final ServerLevel level = player.getLevel();
        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withNearbyServerEntity(final NetworkEvent.Context context, final int id, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        final ServerLevel level = player.getLevel();
        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity) && entity.closerThan(player, 8)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withClientBlockEntityAt(final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (type.isInstance(blockEntity)) {
            callback.accept((T) blockEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withClientEntity(final int id, final Class<T> type, final Consumer<T> callback) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }
}
