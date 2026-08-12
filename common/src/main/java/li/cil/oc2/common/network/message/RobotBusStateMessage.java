/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class RobotBusStateMessage extends AbstractMessage {
    private int entityId;
    private CommonDeviceBusController.BusState value;

    // ------------------------------------------------------------- //

    public RobotBusStateMessage(final Robot robot, final CommonDeviceBusController.BusState value) {
        this.entityId = robot.getId();
        this.value = value;
    }

    public RobotBusStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // ------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readEnum(CommonDeviceBusController.BusState.class);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(value);
    }

    // ------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
                robot -> robot.getVirtualMachine().setBusStateClient(value));
    }
}
