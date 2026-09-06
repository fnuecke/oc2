/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractSession {
    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    // --------------------------------------------------------------------- //

    private final long id = ID_GENERATOR.getAndIncrement();
    private final InetSocketAddress destination;
    private long lastUpdateTime = System.nanoTime();

    // --------------------------------------------------------------------- //

    @Nullable
    private Object userdata;

    protected AbstractSession(final int ipAddress, final short port) {
        destination = new InetSocketAddress(
            InetUtils.toJavaInetAddress(ipAddress), Short.toUnsignedInt(port));
    }

    // --------------------------------------------------------------------- //

    public final long getId() {
        return id;
    }

    public final InetSocketAddress getDestination() {
        return destination;
    }

    public final long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Nullable
    public final Object attachment() {
        return userdata;
    }

    public final void attach(@Nullable final Object userdata) {
        this.userdata = userdata;
    }

    public final void touch() {
        lastUpdateTime = System.nanoTime();
    }

    public final boolean isClosed() {
        return getState().isClosed();
    }

    public abstract SessionState getState();

    public abstract void close();

    public abstract void expire();

    public abstract SessionKey getKey();
}
