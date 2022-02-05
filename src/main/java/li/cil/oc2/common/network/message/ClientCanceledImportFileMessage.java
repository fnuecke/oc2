/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ClientCanceledImportFileMessage extends AbstractMessage {
    private int id;

    ///////////////////////////////////////////////////////////////////

    public ClientCanceledImportFileMessage(final int id) {
        this.id = id;
    }

    public ClientCanceledImportFileMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        id = buffer.readVarInt();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final Supplier<NetworkEvent.Context> context) {
        final ServerPlayer player = context.get().getSender();
        if (player != null) {
            FileImportExportCardItemDevice.cancelImport(player, id);
        }
    }
}
