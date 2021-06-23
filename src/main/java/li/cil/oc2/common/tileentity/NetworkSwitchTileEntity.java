package li.cil.oc2.common.tileentity;

import java.util.Map;
import java.util.HashMap;
import li.cil.oc2.api.capabilities.NetworkInterface;

public final class NetworkSwitchTileEntity extends NetworkHubTileEntity {
    private final Map<Long, NetworkInterface> hostTable = new HashMap<>();

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        validateAdjacentInterfaces();
        long destMac = macToLong(frame, 0);
        long srcMac = macToLong(frame, 6);
        if (hostTable.size() <= 256) {
            hostTable.put(srcMac, source);
        }
        NetworkInterface dest = hostTable.get(destMac);
        if (dest != null) {
            if (dest == source) {
                // if packet is to same port, drop
                return;
            }
            dest.writeEthernetFrame(this, frame, timeToLive - TTL_COST);
        } else {
            super.writeEthernetFrame(source, frame, timeToLive);
        }
    }

    private long macToLong(final byte[] mac, int offset) {
        long ret = 0;
        for (int i = 0; i < 6; i++) {
            ret |= (mac[i + offset] << i);
        }
        return ret;
    }
}
