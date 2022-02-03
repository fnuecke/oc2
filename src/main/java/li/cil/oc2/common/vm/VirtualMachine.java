/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public interface VirtualMachine {
    CommonDeviceBusController.BusState getBusState();

    @OnlyIn(Dist.CLIENT)
    void setBusStateClient(CommonDeviceBusController.BusState value);

    VMRunState getRunState();

    @OnlyIn(Dist.CLIENT)
    void setRunStateClient(VMRunState value);

    @Nullable
    Component getBootError();

    @OnlyIn(Dist.CLIENT)
    void setBootErrorClient(@Nullable Component value);

    boolean isRunning();

    void start();

    void stop();
}
