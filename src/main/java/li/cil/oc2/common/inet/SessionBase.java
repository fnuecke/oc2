package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.common.Config;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SessionBase implements Session {
    private static final AtomicLong idGenerator = new AtomicLong();

    private final long id = idGenerator.getAndIncrement();
    private final InetSocketAddress destination;
    private States state;
    private Instant expireTime;
    private Object userdata;

    public SessionBase(final int ipAddress, final short port) {
        destination = new InetSocketAddress(InetUtils.toJavaInetAddress(ipAddress), Short.toUnsignedInt(port));
        state = States.NEW;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void close() {
        switch (state) {
            case NEW -> state = States.REJECT;
            case ESTABLISHED -> state = States.FINISH;
            default -> throw new IllegalStateException();
        }
    }

    @Override
    public States getState() {
        return state;
    }

    public void setState(final States state) {
        this.state = state;
    }

    public void updateExpireTime() {
        expireTime = Instant.now().plusMillis(Config.defaultSessionLifetimeMs);
    }

    @Nullable
    public Instant getExpireTime() {
        return expireTime;
    }

    @Nullable
    @Override
    public Object getUserdata() {
        return this.userdata;
    }

    @Override
    public void setUserdata(final Object userdata) {
        this.userdata = userdata;
    }

    @Override
    public InetSocketAddress getDestination() {
        return destination;
    }

    public abstract SessionDiscriminator<?> getDiscriminator();
}
