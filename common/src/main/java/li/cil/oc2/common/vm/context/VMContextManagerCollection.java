/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context;

public interface VMContextManagerCollection {
    InterruptManager getInterruptManager();

    MemoryRangeManager getMemoryRangeManager();

    MemoryRangeManager getDeviceRangeManager();

    EventManager getEventManager();
}
