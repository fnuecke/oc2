package li.cil.oc2.common.network;

import li.cil.oc2.common.tile.ComputerTileEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class ComputerTerminalInputMessage extends AbstractComputerTerminalMessage {
    public ComputerTerminalInputMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public ComputerTerminalInputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    public static boolean handleInput(final AbstractComputerTerminalMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            final ServerPlayerEntity player = context.get().getSender();
            if (player == null) return;
            final ServerWorld world = player.getServerWorld();
            // TODO Check if chunk is loaded first.
            final TileEntity tileEntity = world.getTileEntity(message.pos);
            if (!(tileEntity instanceof ComputerTileEntity)) return;
            ((ComputerTileEntity) tileEntity).getTerminal().putInput(ByteBuffer.wrap(message.data));
        });
        return true;
    }
}
