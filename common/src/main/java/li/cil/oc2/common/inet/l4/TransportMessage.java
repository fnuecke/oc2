/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public final class TransportMessage {
    private static final byte DEFAULT_TIME_TO_LIVE = 64;

    // --------------------------------------------------------------------- //

    private int sourceIpAddress;
    private int destinationIpAddress;
    private byte timeToLive = DEFAULT_TIME_TO_LIVE;

    // --------------------------------------------------------------------- //

    @Nullable private ByteBuffer data;
    public void initializeBuffer(final ByteBuffer data) {
        this.data = data;
    }

    public void updateIpv4(final int sourceIpAddress, final int destinationIpAddress, final byte timeToLive) {
        this.sourceIpAddress = sourceIpAddress;
        this.destinationIpAddress = destinationIpAddress;
        this.timeToLive = timeToLive;
    }

    public void updateIpv4(final int sourceIpAddress, final int destinationIpAddress) {
        updateIpv4(sourceIpAddress, destinationIpAddress, DEFAULT_TIME_TO_LIVE);
    }

    public byte getTimeToLive() {
        return timeToLive;
    }

    public int getSourceIpv4Address() {
        return sourceIpAddress;
    }

    public int getDestinationIpv4Address() {
        return destinationIpAddress;
    }

    public ByteBuffer getData() {
        final ByteBuffer data = this.data;
        if (data == null) {
            throw new IllegalStateException("No buffer set on this message.");
        }
        return data;
    }
}
