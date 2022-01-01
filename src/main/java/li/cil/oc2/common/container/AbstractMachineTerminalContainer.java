package li.cil.oc2.common.container;

import li.cil.oc2.common.vm.Terminal;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;

import java.nio.ByteBuffer;

public abstract class AbstractMachineTerminalContainer extends AbstractMachineContainer {
    protected AbstractMachineTerminalContainer(final MenuType<?> type, final int id, final IntPrecisionContainerData energyInfo) {
        super(type, id, energyInfo);
    }

    ///////////////////////////////////////////////////////////////////

    public abstract void switchToTerminal();

    public abstract Terminal getTerminal();

    public abstract void sendTerminalInputToServer(final ByteBuffer input);
}
