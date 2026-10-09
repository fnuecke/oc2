/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.PowerAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class ComputerPowerMessage extends AbstractMessage {
    private BlockPos pos;
    private PowerAction action;

    // --------------------------------------------------------------------- //

    public ComputerPowerMessage(final ComputerBlockEntity computer, final PowerAction action) {
        this.pos = computer.getBlockPos();
        this.action = action;
    }

    public ComputerPowerMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        action = buffer.readEnum(PowerAction.class);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(action);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, ComputerBlockEntity.class,
            (player, computer) -> {
                switch (action) {
                    case START -> computer.start();
                    case SHUTDOWN -> computer.shutdown();
                    case REBOOT -> computer.reboot();
                    case POWER_OFF -> computer.stop();
                }
            });
    }
}
