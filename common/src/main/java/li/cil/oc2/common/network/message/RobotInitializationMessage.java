/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import javax.annotation.Nullable;
import java.util.Optional;

public final class RobotInitializationMessage extends AbstractMessage {
    private int entityId;
    private CommonDeviceBusController.BusState busState;
    private VMRunState runState;
    @Nullable
    private Component bootError;
    private CompoundTag terminal;

    // --------------------------------------------------------------------- //

    public RobotInitializationMessage(final Robot robot) {
        this.entityId = robot.getId();
        this.busState = robot.getVirtualMachine().getBusState();
        this.runState = robot.getVirtualMachine().getRunState();
        this.bootError = robot.getVirtualMachine().getBootError();
        final Terminal robotTerminal = robot.getTerminal();
        synchronized (robotTerminal) {
            this.terminal = NBTSerialization.serialize(robotTerminal);
        }
    }

    public RobotInitializationMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        busState = buffer.readEnum(CommonDeviceBusController.BusState.class);
        runState = buffer.readEnum(VMRunState.class);
        bootError = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buffer).orElse(null);
        terminal = buffer.readNbt();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(busState);
        buffer.writeEnum(runState);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buffer, Optional.ofNullable(bootError));
        buffer.writeNbt(terminal);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
            robot -> {
                robot.getVirtualMachineClientState().setBusStateClient(busState);
                robot.getVirtualMachineClientState().setRunStateClient(runState);
                robot.getVirtualMachineClientState().setBootErrorClient(bootError);
                NBTSerialization.deserialize(terminal, robot.getTerminal());
            });
    }
}
