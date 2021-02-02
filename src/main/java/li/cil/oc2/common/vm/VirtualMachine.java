package li.cil.oc2.common.vm;

import li.cil.oc2.common.bus.AbstractDeviceBusController;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public interface VirtualMachine {
    AbstractDeviceBusController.BusState getBusState();

    @OnlyIn(Dist.CLIENT)
    void setBusStateClient(AbstractDeviceBusController.BusState value);

    VMRunState getRunState();

    @OnlyIn(Dist.CLIENT)
    void setRunStateClient(VMRunState value);

    @Nullable
    ITextComponent getBootError();

    @OnlyIn(Dist.CLIENT)
    void setBootErrorClient(ITextComponent value);

    boolean isRunning();

    void start();

    void stop();

    void joinWorkerThread();

}
