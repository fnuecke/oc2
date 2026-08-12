/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ProjectorStateMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean isMounted;
    private boolean hasEnergy;

    // ------------------------------------------------------------- //

    public ProjectorStateMessage(final ProjectorBlockEntity projector, final boolean isMounted, final boolean hasEnergy) {
        this.pos = projector.getBlockPos();
        this.isMounted = isMounted;
        this.hasEnergy = hasEnergy;
    }

    public ProjectorStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // ------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        isMounted = buffer.readBoolean();
        hasEnergy = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(isMounted);
        buffer.writeBoolean(hasEnergy);
    }

    // ------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
            projector -> projector.applyProjectorStateClient(isMounted, hasEnergy));
    }
}
