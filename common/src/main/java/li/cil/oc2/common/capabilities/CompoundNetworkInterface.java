/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.capabilities.NetworkInterface;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class CompoundNetworkInterface implements NetworkInterface {
    private static final int LOCAL_TIME_TO_LIVE = 1; // passing packet internally

    // --------------------------------------------------------------------- //

    private final NetworkInterface[] interfaces;
    private int nextIndex; // for round-robin reading

    // --------------------------------------------------------------------- //

    private CompoundNetworkInterface(final List<NetworkInterface> interfaces) {
        this.interfaces = interfaces.toArray(new NetworkInterface[0]);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public static NetworkInterface of(final List<CapabilityProvider> providers, @Nullable final Direction side) {
        NetworkInterface first = null;
        List<NetworkInterface> all = null;

        for (final CapabilityProvider provider : providers) {
            final NetworkInterface networkInterface = provider.getCapability(Capabilities.NETWORK_INTERFACE, side);
            if (networkInterface == null) {
                continue;
            }

            if (first == null) {
                first = networkInterface;
            } else {
                if (all == null) {
                    all = new ArrayList<>();
                    all.add(first);
                }
                all.add(networkInterface);
            }
        }

        return all != null ? new CompoundNetworkInterface(all) : first;
    }

    // --------------------------------------------------------------------- //

    @Nullable
    @Override
    public byte[] readEthernetFrame() {
        for (int i = 0; i < interfaces.length; i++) {
            final NetworkInterface networkInterface = interfaces[nextIndex];
            nextIndex = (nextIndex + 1) % interfaces.length;

            final byte[] frame = networkInterface.readEthernetFrame();
            if (frame != null) {
                for (final NetworkInterface sibling : interfaces) {
                    if (sibling != networkInterface) {
                        sibling.writeEthernetFrame(networkInterface, frame, LOCAL_TIME_TO_LIVE);
                    }
                }

                return frame;
            }
        }

        return null;
    }

    @Override
    public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        for (final NetworkInterface networkInterface : interfaces) {
            networkInterface.writeEthernetFrame(source, frame, timeToLive);
        }
    }
}
