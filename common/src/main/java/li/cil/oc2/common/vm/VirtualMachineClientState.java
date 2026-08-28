/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public interface VirtualMachineClientState {
    @Environment(EnvType.CLIENT)
    void setBusStateClient(CommonDeviceBusController.BusState value);

    @Environment(EnvType.CLIENT)
    void setRunStateClient(VMRunState value);

    @Environment(EnvType.CLIENT)
    void setBootErrorClient(@Nullable Component value);
}
