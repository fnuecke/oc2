package li.cil.oc2.common.container;

import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

public abstract class AbstractMachineContainer extends AbstractContainer {
    protected static final int ENERGY_INFO_SIZE = 3;

    public static final int ENERGY_STORED_INDEX = 0;
    public static final int ENERGY_CAPACITY_INDEX = 1;
    public static final int ENERGY_CONSUMPTION_INDEX = 2;

    ///////////////////////////////////////////////////////////////////

    private final IIntArray energyInfo;

    ///////////////////////////////////////////////////////////////////

    protected AbstractMachineContainer(final ContainerType<?> type, final int id, final IIntArray energyInfo) {
        super(type, id);
        this.energyInfo = energyInfo;

        checkContainerDataCount(energyInfo, ENERGY_INFO_SIZE);
        addDataSlots(energyInfo);
    }

    ///////////////////////////////////////////////////////////////////

    public abstract void switchToInventory();

    public abstract VirtualMachine getVirtualMachine();

    public abstract void sendPowerStateToServer(final boolean value);

    public int getEnergy() {
        return energyInfo.get(ENERGY_STORED_INDEX);
    }

    public int getEnergyCapacity() {
        return energyInfo.get(ENERGY_CAPACITY_INDEX);
    }

    public int getEnergyConsumption() {
        return energyInfo.get(ENERGY_CONSUMPTION_INDEX);
    }

    ///////////////////////////////////////////////////////////////////

    protected static IIntArray createEnergyInfo() {
        return new IntArray(ENERGY_INFO_SIZE);
    }
}
