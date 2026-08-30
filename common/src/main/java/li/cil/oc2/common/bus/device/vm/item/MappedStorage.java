/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.common.bus.device.util.OptionalAddress;
import li.cil.oc2.common.bus.device.util.OptionalInterrupt;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;

import javax.annotation.Nullable;
import java.io.IOException;

public interface MappedStorage {
    MemoryMappedDevice getDevice();

    VMDeviceLoadResult claim(VMContext context, OptionalAddress address, OptionalInterrupt interrupt);

    void setBlockDevice(@Nullable BlockDevice block) throws IOException;

    void close() throws IOException;
}
