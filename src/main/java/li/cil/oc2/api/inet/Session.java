package li.cil.oc2.api.inet;

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

    enum States {
        NEW, ESTABLISHED, FINISH, REJECT, EXPIRED
    }
}
