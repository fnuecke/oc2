/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.managed;

import li.cil.oc2.api.bus.device.vm.context.VMRuntime;

public final class ManagedVMRuntime implements VMRuntime {
    private final VMRuntime parent;
    private boolean isValid = true;

    // --------------------------------------------------------------------- //

    public ManagedVMRuntime(final VMRuntime parent) {
        this.parent = parent;
    }

    // --------------------------------------------------------------------- //

    public void invalidate() {
        isValid = false;
    }

    @Override
    public void join() {
        if (!isValid) {
            throw new IllegalStateException();
        }

        parent.join();
    }
}
