package li.cil.oc2.common.network;

import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MessageUtils {
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withServerBlockEntityAt(final Supplier<NetworkEvent.Context> context, final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayerEntity player = context.get().getSender();
        if (player == null) {
            return;
        }

        final ServerWorld world = player.getLevel();
        final BlockEntity tileEntity = WorldUtils.getBlockEntityIfChunkExists(world, pos);
        if (type.isInstance(tileEntity)) {
            callback.accept((T) tileEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> void withServerEntity(final Supplier<NetworkEvent.Context> context, final int id, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayerEntity player = context.get().getSender();
        if (player == null) {
            return;
        }

        final ServerWorld world = player.getLevel();
        final Entity entity = world.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withClientBlockEntityAt(final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ClientWorld world = Minecraft.getInstance().level;
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
        final ClientWorld world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }

        final Entity entity = world.getEntity(id);
        if (type.isInstance(entity)) {
            callback.accept((T) entity);
        }
    }
}
