/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class RobotRunStateMessage extends AbstractMessage {
    private int entityId;
    private VMRunState value;

    // --------------------------------------------------------------------- //

    public RobotRunStateMessage(final Robot robot, final VMRunState value) {
        this.entityId = robot.getId();
        this.value = value;
    }

    public RobotRunStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readEnum(VMRunState.class);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(value);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
            robot -> robot.getVirtualMachineClientState().setRunStateClient(value));
    }
}
