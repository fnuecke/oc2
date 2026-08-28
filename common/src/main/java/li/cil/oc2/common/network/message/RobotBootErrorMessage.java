/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import javax.annotation.Nullable;
import java.util.Optional;

public final class RobotBootErrorMessage extends AbstractMessage {
    private int entityId;
    @Nullable
    private Component value;

    // --------------------------------------------------------------------- //

    public RobotBootErrorMessage(final Robot robot, @Nullable final Component value) {
        this.entityId = robot.getId();
        this.value = value;
    }

    public RobotBootErrorMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        value = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buffer).orElse(null);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buffer, Optional.ofNullable(value));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
            robot -> robot.getVirtualMachineClientState().setBootErrorClient(value));
    }
}
