/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.object.Callback;

public final class Pingable {
    @Callback(synchronize = false)
    public int ping() {
        return 1;
    }
}
