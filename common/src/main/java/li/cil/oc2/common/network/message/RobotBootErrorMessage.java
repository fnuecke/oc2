/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import dev.architectury.networking.NetworkManager;

import javax.annotation.Nullable;

public final class RobotBootErrorMessage extends AbstractMessage {
    private int entityId;
    private Component value;

    ///////////////////////////////////////////////////////////////////

    public RobotBootErrorMessage(final Robot robot, @Nullable final Component value) {
        this.entityId = robot.getId();
        this.value = value;
    }

    public RobotBootErrorMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        value = ComponentSerialization.STREAM_CODEC.decode(buffer);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        ComponentSerialization.STREAM_CODEC.encode(buffer, value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
            robot -> robot.getVirtualMachine().setBootErrorClient(value));
    }
}
