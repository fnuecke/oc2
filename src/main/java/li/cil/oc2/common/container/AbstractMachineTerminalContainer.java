package li.cil.oc2.common.container;

import li.cil.oc2.common.vm.Terminal;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;

import java.nio.ByteBuffer;

public abstract class AbstractMachineTerminalContainer extends AbstractMachineContainer {
    protected AbstractMachineTerminalContainer(final ContainerType<?> type, final int id, final IIntArray energyInfo) {
        super(type, id, energyInfo);
    }

    ///////////////////////////////////////////////////////////////////

    public abstract Terminal getTerminal();

    public abstract void sendTerminalInputToServer(final ByteBuffer input);
}
