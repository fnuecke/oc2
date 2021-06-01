package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.Component;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class RobotInitializationMessage {
    private int entityId;
    private CommonDeviceBusController.BusState busState;
    private VMRunState runState;
    private Component bootError;
    private CompoundTag terminal;

    ///////////////////////////////////////////////////////////////////

    public RobotInitializationMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
        this.busState = robot.getVirtualMachine().getBusState();
        this.runState = robot.getVirtualMachine().getRunState();
        this.bootError = robot.getVirtualMachine().getBootError();
        this.terminal = NBTSerialization.serialize(robot.getTerminal());
    }

    public RobotInitializationMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RobotInitializationMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientEntity(message.entityId, RobotEntity.class,
                (robot) -> {
                    robot.getVirtualMachine().setBusStateClient(message.busState);
                    robot.getVirtualMachine().setRunStateClient(message.runState);
                    robot.getVirtualMachine().setBootErrorClient(message.bootError);
                    NBTSerialization.deserialize(message.terminal, robot.getTerminal());
                }));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        busState = buffer.readEnum(CommonDeviceBusController.BusState.class);
        runState = buffer.readEnum(VMRunState.class);
        bootError = buffer.readComponent();
        terminal = buffer.readNbt();
    }

    public static void toBytes(final RobotInitializationMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeEnum(message.busState);
        buffer.writeEnum(message.runState);
        buffer.writeComponent(message.bootError);
        buffer.writeNbt(message.terminal);
    }
}
