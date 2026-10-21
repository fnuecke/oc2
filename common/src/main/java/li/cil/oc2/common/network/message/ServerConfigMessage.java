/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.GameInstance;
import li.cil.oc2.common.ConfigManager;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class ServerConfigMessage extends AbstractMessage {
    private String fileName;
    private byte[] contents;

    // --------------------------------------------------------------------- //

    public ServerConfigMessage(final String fileName, final byte[] contents) {
        this.fileName = fileName;
        this.contents = contents;
    }

    public ServerConfigMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        fileName = buffer.readUtf();
        contents = buffer.readByteArray();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(fileName);
        buffer.writeByteArray(contents);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        if (GameInstance.getServer() != null) { // Skip on integrated server.
            return;
        }

        ConfigManager.applyConfigFile(fileName, contents);
    }
}
