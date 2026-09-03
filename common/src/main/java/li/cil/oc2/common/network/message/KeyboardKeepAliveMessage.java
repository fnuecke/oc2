/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.KeyboardBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * We don't have a container for the keyboard, just the screen, so we track player
 * presence using regular keep-alive messages instead. It is what it is.
 */
public final class KeyboardKeepAliveMessage extends AbstractMessage {
    private BlockPos pos;

    // --------------------------------------------------------------------- //

    public KeyboardKeepAliveMessage(final KeyboardBlockEntity keyboard) {
        this.pos = keyboard.getBlockPos();
    }

    public KeyboardKeepAliveMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, KeyboardBlockEntity.class,
            (player, keyboard) -> keyboard.handleUsedBy(player));
    }
}
