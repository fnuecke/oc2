package li.cil.circuity.api.vm.device.rtc;

import li.cil.circuity.api.vm.device.Device;

public interface RealTimeCounter extends Device {
    long getTime();

    int getFrequency();
}
