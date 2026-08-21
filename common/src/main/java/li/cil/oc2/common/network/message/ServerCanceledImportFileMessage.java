/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.bus.device.rpc.item.FileImportExportCardItemDevice;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;


public final class ServerCanceledImportFileMessage extends AbstractMessage {
    private int id;

    // --------------------------------------------------------------------- //

    public ServerCanceledImportFileMessage(final int id) {
        this.id = id;
    }

    public ServerCanceledImportFileMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        id = buffer.readVarInt();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof final ServerPlayer player) {
            FileImportExportCardItemDevice.cancelImport(player, id);
        }
    }
}
