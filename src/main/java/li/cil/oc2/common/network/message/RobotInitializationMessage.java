package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public final class RobotInitializationMessage extends AbstractMessage {
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

    public RobotInitializationMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        busState = buffer.readEnum(CommonDeviceBusController.BusState.class);
        runState = buffer.readEnum(VMRunState.class);
        bootError = buffer.readComponent();
        terminal = buffer.readNbt();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(busState);
        buffer.writeEnum(runState);
        buffer.writeComponent(bootError);
        buffer.writeNbt(terminal);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientEntity(entityId, RobotEntity.class,
                (robot) -> {
                    robot.getVirtualMachine().setBusStateClient(busState);
                    robot.getVirtualMachine().setRunStateClient(runState);
                    robot.getVirtualMachine().setBootErrorClient(bootError);
                    NBTSerialization.deserialize(terminal, robot.getTerminal());
                });
    }
}
