package li.cil.oc2.api.inet.session;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Instant;

public interface Session {
    long getId();

    void close();

    States getState();

    @Nullable
    Object getAttachment();

    void setAttachment(@Nullable final Object userdata);

    InetSocketAddress getDestination();

    Instant getLastUpdateTime();

    default boolean isClosed() {
        return switch (getState()) {
            case FINISH, REJECT, EXPIRED -> true;
            default -> false;
        };
    }

    enum States {
        NEW, ESTABLISHED, FINISH, REJECT, EXPIRED
    }
}
