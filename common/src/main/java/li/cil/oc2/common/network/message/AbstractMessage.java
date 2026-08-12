/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.commons.lang3.NotImplementedException;

public abstract class AbstractMessage implements CustomPacketPayload {
    protected AbstractMessage() {
    }

    protected AbstractMessage(final RegistryFriendlyByteBuf buffer) {
        fromBytes(buffer);
    }

    // ------------------------------------------------------------- //

    public abstract void fromBytes(final RegistryFriendlyByteBuf buffer);

    public abstract void toBytes(final RegistryFriendlyByteBuf buffer);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return Network.getMessageType(getClass());
    }

    // ------------------------------------------------------------- //

    protected void handleMessage(final NetworkManager.PacketContext context) {
        throw new NotImplementedException("Message does not implement handleMessage().");
    }

    public void handle(final NetworkManager.PacketContext context) {
        handleMessage(context);
    }
}
