/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public interface SessionLayer {
    interface Receiver {
        @Nullable
        ByteBuffer receive(AbstractSession session);

        void cancel();
    }

    // --------------------------------------------------------------------- //

    default void onTick() {
    }

    default void onStop() {
    }

    // --------------------------------------------------------------------- //

    void receiveSession(Receiver receiver);

    void sendSession(AbstractSession session, @Nullable ByteBuffer data);
}
