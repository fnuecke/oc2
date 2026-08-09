/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.annotation.Nullable;

public interface VirtualMachine {
    CommonDeviceBusController.BusState getBusState();

    @Environment(EnvType.CLIENT)
    void setBusStateClient(CommonDeviceBusController.BusState value);

    VMRunState getRunState();

    @Environment(EnvType.CLIENT)
    void setRunStateClient(VMRunState value);

    @Nullable
    Component getBootError();

    @Environment(EnvType.CLIENT)
    void setBootErrorClient(@Nullable Component value);

    @Nullable
    Component getError();

    boolean isRunning();

    void start();

    void stop();
}
