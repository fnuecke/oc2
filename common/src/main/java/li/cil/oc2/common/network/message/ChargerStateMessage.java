/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.ChargerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class ChargerStateMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean isPowered;

    // --------------------------------------------------------------------- //

    public ChargerStateMessage(final ChargerBlockEntity charger, final boolean isPowered) {
        this.pos = charger.getBlockPos();
        this.isPowered = isPowered;
    }

    public ChargerStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        isPowered = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(isPowered);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ChargerBlockEntity.class,
            charger -> charger.setHasEnergyClient(isPowered));
    }
}
