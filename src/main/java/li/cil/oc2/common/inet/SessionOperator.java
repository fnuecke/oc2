package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.Session;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public interface SessionOperator extends Session {
    @Nullable
    byte[] nextReceive();

    void nextSent(final byte[] data);

    default void nextSent(final ByteBuffer data) {
        final byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        nextSent(bytes);
    }
}
