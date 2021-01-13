package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.vm.VirtualMachineState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ComputerRunStateMessage {
    private BlockPos pos;
    private VirtualMachineState.RunState value;

    ///////////////////////////////////////////////////////////////////

    public ComputerRunStateMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getPos();
        this.value = tileEntity.getState().getRunState();
    }

    public ComputerRunStateMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ComputerRunStateMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.getState().setRunStateClient(message.value)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        value = buffer.readEnumValue(VirtualMachineState.RunState.class);
    }

    public static void toBytes(final ComputerRunStateMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnumValue(message.value);
    }
}
