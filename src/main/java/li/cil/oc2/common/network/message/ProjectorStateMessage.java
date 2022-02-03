/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class ProjectorStateMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean isProjecting;

    ///////////////////////////////////////////////////////////////////

    public ProjectorStateMessage(final ProjectorBlockEntity projector, final boolean isProjecting) {
        this.pos = projector.getBlockPos();
        this.isProjecting = isProjecting;
    }

    public ProjectorStateMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        isProjecting = buffer.readBoolean();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(isProjecting);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
            projector -> projector.setProjecting(isProjecting));
    }
}
