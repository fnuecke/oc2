/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import li.cil.oc2.api.bus.device.object.ObjectDevice;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a single method than can be exposed by an {@link IODevice}.
 * <p>
 * The easiest and hence recommended way of generating an implementation of this
 * interface is to use the {@link ObjectDevice} class.
 * <p>
 * This is the mid-level API targeting CP/M guests. Also see the high-level API equivalent for
 * Linux guests, {@link li.cil.oc2.api.bus.device.rpc.RPCMethod}.
 *
 * @see IODevice
 * @see ObjectDevice
 */
public interface IOMethod {
    /**
     * The code identifying this method to the guest.
     *
     * @return the method code.
     */
    int getCode();

    /**
     * When {@code true}, invocations of this method will be synchronized to the main thread.
     *
     * @return {@code true} when to be executed on main thread; {@code false} otherwise.
     */
    boolean isSynchronized();

    /**
     * Called to run this method.
     * <p>
     * <b>Important:</b> methods are expected to not irrevocably corrupt internal
     * state, even when they throw an exception. As such, implementations should
     * perform internal error handling to prevent state corruption and only throw
     * exceptions to communicate that an error happened during the invocation.
     * <p>
     * Reading past the end of {@code arguments} yields {@code -1}, as usual; implementations are
     * responsible for validating that they got the arguments they need. Throwing
     * {@link IllegalArgumentException} or {@link IllegalStateException} tells the guest it called
     * the method wrong; anything else is reported as an internal error.
     *
     * @param arguments the bytes the guest wrote for this call.
     * @param results   the bytes to hand back to the guest.
     * @throws Throwable if the parameters did not match or something inside the
     *                   method caused an exception. The caller is responsible for
     *                   catching these and passing them on appropriately.
     */
    void invoke(InputStream arguments, OutputStream results) throws Throwable;
}
