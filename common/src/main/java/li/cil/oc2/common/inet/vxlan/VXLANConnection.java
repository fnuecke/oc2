/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.vxlan;

import li.cil.oc2.common.inet.GatewayConnection;
import li.cil.oc2.common.inet.InternetAdapter;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VXLANConnection implements GatewayConnection  {

    private final InternetAdapter adapter;
    private final InetSocketAddress peer;
    private final int vni;

    private final AtomicBoolean stopRequested = new AtomicBoolean();

    // --------------------------------------------------------------------- //

    VXLANConnection(InternetAdapter adapter, InetSocketAddress peer, int vni) {
        this.adapter = adapter;
        this.peer = peer;
        this.vni = vni;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void stop() {
        stopRequested.set(true);
    }

    // --------------------------------------------------------------------- //

    boolean isStopped() {
        return stopRequested.get();
    }

    InternetAdapter getAdapter() {
        return adapter;
    }

    InetSocketAddress getPeer() {
        return peer;
    }

    int getVni() {
        return vni;
    }
}
