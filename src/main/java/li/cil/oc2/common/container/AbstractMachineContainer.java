/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.world.inventory.MenuType;

public abstract class AbstractMachineContainer extends AbstractContainer {
    protected static final int ENERGY_INFO_SIZE = 3;

    public static final int ENERGY_STORED_INDEX = 0;
    public static final int ENERGY_CAPACITY_INDEX = 1;
    public static final int ENERGY_CONSUMPTION_INDEX = 2;

    ///////////////////////////////////////////////////////////////////

    private final IntPrecisionContainerData energyInfo;

    ///////////////////////////////////////////////////////////////////

    protected AbstractMachineContainer(final MenuType<?> type, final int id, final IntPrecisionContainerData energyInfo) {
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
        return energyInfo.getInt(ENERGY_STORED_INDEX);
    }

    public int getEnergyCapacity() {
        return energyInfo.getInt(ENERGY_CAPACITY_INDEX);
    }

    public int getEnergyConsumption() {
        return energyInfo.getInt(ENERGY_CONSUMPTION_INDEX);
    }

    ///////////////////////////////////////////////////////////////////

    protected static IntPrecisionContainerData createClientEnergyInfo() {
        return new IntPrecisionContainerData.Client(ENERGY_INFO_SIZE);
    }
}
