/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.ProjectorLoadBalancer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class ProjectorRequestFramebufferMessage extends AbstractMessage {
    private BlockPos pos;

    ///////////////////////////////////////////////////////////////////

    public ProjectorRequestFramebufferMessage(final ProjectorBlockEntity projector) {
        this.pos = projector.getBlockPos();
    }

    public ProjectorRequestFramebufferMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerBlockEntity(context, pos, ProjectorBlockEntity.class,
            (player, projector) -> ProjectorLoadBalancer.updateWatcher(projector, player));
    }
}
