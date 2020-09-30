package li.cil.oc2.common.network;

import li.cil.oc2.common.tile.ComputerTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class TerminalBlockOutputMessage extends AbstractTerminalBlockMessage {
    public TerminalBlockOutputMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public TerminalBlockOutputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    public static boolean handleOutput(final AbstractTerminalBlockMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            final ClientWorld world = Minecraft.getInstance().world;
            if (world == null) return;
            final TileEntity tileEntity = world.getTileEntity(message.pos);
            if (!(tileEntity instanceof ComputerTileEntity)) return;
            ((ComputerTileEntity) tileEntity).getTerminal().putOutput(ByteBuffer.wrap(message.data));
        });
        return true;
    }
}
