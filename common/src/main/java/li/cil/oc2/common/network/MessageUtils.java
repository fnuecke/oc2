/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MessageUtils {
    private static final int INTERACTION_DISTANCE = 8;

    // ------------------------------------------------------------- //

    public static <T extends BlockEntity> void withNearbyServerBlockEntityForInteraction(final NetworkManager.PacketContext context, final BlockPos pos, final Class<T> type, final BiConsumer<ServerPlayer, T> callback) {
        withNearbyServerBlockEntity(context, pos, type, INTERACTION_DISTANCE, callback);
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withNearbyServerBlockEntity(final NetworkManager.PacketContext context, final BlockPos pos, final Class<T> type, final int maxDistance, final BiConsumer<ServerPlayer, T> callback) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)
                || !pos.closerToCenterThan(player.position(), maxDistance)) {
            return;
        }

        final ServerLevel level = player.serverLevel();
        final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(level, pos);
        if (type.isInstance(blockEntity)) {
            callback.accept(player, (T) blockEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withServerEntity(final NetworkManager.PacketContext context, final int id, final Class<T> type, final Consumer<T> callback) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
            return;
        }

        final ServerLevel level = player.serverLevel();
        final Entity entity = level.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withNearbyServerEntity(final NetworkManager.PacketContext context, final int id, final Class<T> type, final Consumer<T> callback) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
            return;
        }

        final ServerLevel level = player.serverLevel();
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
