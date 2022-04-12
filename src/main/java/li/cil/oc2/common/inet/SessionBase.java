package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.Session;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SessionBase implements Session {
    private static final AtomicLong idGenerator = new AtomicLong();

    private final long id = idGenerator.getAndIncrement();
    private final InetSocketAddress destination;
    private Instant lastUpdateTime = Instant.now();
    @Nullable
    private Object attachment;

    public SessionBase(final int ipAddress, final short port) {
        destination = new InetSocketAddress(InetUtils.toJavaInetAddress(ipAddress), Short.toUnsignedInt(port));
    }

    @Override
    public long getId() {
        return id;
    }

    public void update() {
        lastUpdateTime = Instant.now();
    }

    @Override
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Nullable
    @Override
    public Object getAttachment() {
        return this.attachment;
    }

    @Override
    public void setAttachment(@Nullable final Object userdata) {
        this.attachment = userdata;
    }

    @Override
    public InetSocketAddress getDestination() {
        return destination;
    }

    public abstract SessionDiscriminator<?> getDiscriminator();

    public abstract void expire();
}
