/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.AbstractRemovableMediaBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractRemovableMediaMessage extends AbstractMessage {
    private BlockPos pos;
    private ItemStack media;

    // --------------------------------------------------------------------- //

    protected AbstractRemovableMediaMessage(final AbstractRemovableMediaBlockEntity<?> drive) {
        this.pos = drive.getBlockPos();
        this.media = drive.getMedia().copy();
    }

    protected AbstractRemovableMediaMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    protected abstract Class<? extends AbstractRemovableMediaBlockEntity<?>> getBlockEntityType();

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        media = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, media);
    }

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, getBlockEntityType(), drive -> drive.setMediaClient(media));
    }
}
