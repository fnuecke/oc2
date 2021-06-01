package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
public interface VirtualMachine {
    CommonDeviceBusController.BusState getBusState();


    void setBusStateClient(CommonDeviceBusController.BusState value);

    VMRunState getRunState();


    void setRunStateClient(VMRunState value);

    @Nullable
    Component getBootError();


    void setBootErrorClient(Component value);

    boolean isRunning();

    void start();

    void stop();

    void joinWorkerThread();

}
