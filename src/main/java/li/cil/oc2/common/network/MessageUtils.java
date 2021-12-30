package li.cil.oc2.common.network;

import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;

public final class MessageUtils {
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withNearbyServerTileEntityAt(final NetworkEvent.Context context, final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayer player = context.getSender();
        if (player == null || !pos.closerThan(player.position(), 8)) {
            return;
        }

        final ServerLevel world = player.getLevel();
        final BlockEntity tileEntity = WorldUtils.getBlockEntityIfChunkExists(world, pos);
        if (type.isInstance(tileEntity)) {
            callback.accept((T) tileEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withServerEntity(final NetworkEvent.Context context, final int id, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        final ServerLevel world = player.getLevel();
        final Entity entity = world.getEntity(id);
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

        final ServerLevel world = player.getLevel();
        final Entity entity = world.getEntity(id);
        if (type.isInstance(entity) && entity.closerThan(player, 8)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withClientTileEntityAt(final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (type.isInstance(tileEntity)) {
            callback.accept((T) tileEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withClientEntity(final int id, final Class<T> type, final Consumer<T> callback) {
        final ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        final Entity entity = world.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }
}
