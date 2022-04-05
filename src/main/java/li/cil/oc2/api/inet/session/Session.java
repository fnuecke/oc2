package li.cil.oc2.api.inet.session;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;

public interface Session {
    long getId();

    void close();

    States getState();

    @Nullable
    Object getUserdata();

    void setUserdata(final Object userdata);

    InetSocketAddress getDestination();

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
