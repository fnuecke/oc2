/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

public final class SessionLimits {
    private final int perGateway;
    private final int total;
    private int used;

    // --------------------------------------------------------------------- //

    public SessionLimits(final int perGateway, final int total) {
        this.perGateway = perGateway;
        this.total = total;
    }

    // --------------------------------------------------------------------- //

    public boolean tryAcquire(final int usedByGateway, final int gatewayCount) {
        if (used >= total || usedByGateway >= shareFor(gatewayCount)) {
            return false;
        }
        ++used;
        return true;
    }

    public int shareFor(final int gatewayCount) {
        if (gatewayCount <= 1) {
            return perGateway;
        }
        return Math.clamp(total / gatewayCount, 1, perGateway);
    }

    public void release() {
        if (used > 0) {
            --used;
        }
    }

    public int getUsed() {
        return used;
    }
}
