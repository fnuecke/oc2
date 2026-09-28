/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Describes an invocation of an {@link IOMethod}.
 */
public interface IOInvocation {
    /**
     * The bytes the guest wrote for this call.
     *
     * @return the arguments.
     */
    InputStream getInput();

    /**
     * The bytes to hand back to the guest.
     *
     * @return the results.
     */
    OutputStream getOutput();

    /**
     * Drains energy from the computer that made this call.
     * <p>
     * Only possible in synchronized invocations, see {@link IOMethod#isSynchronized()}.
     *
     * @param amount the amount of energy to drain.
     * @return whether the energy was successfully drained.
     * @throws IllegalArgumentException if {@code amount} is negative.
     * @throws IllegalStateException    if the invocation is not synchronized.
     */
    boolean consumeEnergy(int amount);
}
