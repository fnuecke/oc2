package li.cil.oc2.api.inet;

import java.nio.ByteBuffer;

public interface StreamSession extends Session {
    ByteBuffer getSendBuffer();
    ByteBuffer getReceiveBuffer();

    void connect();
}
