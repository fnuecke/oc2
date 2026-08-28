/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public interface VirtualMachine {
    CommonDeviceBusController.BusState getBusState();

    VMRunState getRunState();

    @Nullable
    Component getBootError();

    @Nullable
    Component getError();

    boolean isRunning();

    void start();

    void stop();
}
