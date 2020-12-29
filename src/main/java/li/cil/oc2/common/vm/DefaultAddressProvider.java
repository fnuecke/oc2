package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.VMDevice;

import java.util.OptionalLong;

public interface DefaultAddressProvider {
    OptionalLong getDefaultAddress(final VMDevice wrapper);
}
