package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.VMDevice;

import java.util.OptionalLong;

public interface BaseAddressProvider {
    OptionalLong getBaseAddress(final VMDevice wrapper);
}
