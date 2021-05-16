package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.item.CloudInterfaceCardItemDevice;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientCanceledImportFileMessage {
    private int id;

    ///////////////////////////////////////////////////////////////////

    public ClientCanceledImportFileMessage(final int id) {
        this.id = id;
    }

    public ClientCanceledImportFileMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ClientCanceledImportFileMessage message, final Supplier<NetworkEvent.Context> context) {
        CloudInterfaceCardItemDevice.cancelImport(context.get().getSender(), message.id);
        return true;
    }

    public static void toBytes(final ClientCanceledImportFileMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.id);
    }

    public void fromBytes(final PacketBuffer buffer) {
        id = buffer.readVarInt();
    }
}
