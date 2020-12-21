package li.cil.oc2.common.network;

import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public final class MessageUtils {
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withServerTileEntityAt(final Network.MessageContext context, final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ServerPlayerEntity player = context.getServerPlayer();
        if (player == null) {
            return;
        }

        final ServerWorld world = player.getServerWorld();
        final BlockEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(world, pos);
        if (type.isInstance(tileEntity)) {
            callback.accept((T) tileEntity);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> void withClientTileEntityAt(final BlockPos pos, final Class<T> type, final Consumer<T> callback) {
        final ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }

        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (type.isInstance(tileEntity)) {
            callback.accept((T) tileEntity);
        }
    }
}
