/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import li.cil.oc2.api.bus.device.object.ObjectDevice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Utility annotation to allow generating lists of {@link IOMethod}s using
 * {@link IOCallbacks#collectMethods(Object)}.
 * <p>
 * Intended to be used in classes instances of which are used as a target of {@link ObjectDevice}.
 * <p>
 * Annotated methods must return {@code void} and accept one of four parameter lists:
 * {@code ()}, {@code (InputStream)}, {@code (OutputStream)} or {@code (InputStream, OutputStream)}.
 * Arguments are read from the {@link java.io.InputStream}, results written to the
 * {@link java.io.OutputStream}; either may be omitted when unused. As such, the guest
 * must be aware of the device's protocol to use it correctly.
 * <p>
 * The declaring class must carry an {@link IOName}, since the guest sees devices by name.
 * <p>
 * This is the mid-level API for CP/M guests. Also see {@link li.cil.oc2.api.bus.device.object.Callback}
 * for the high-level API for Linux guests.
 *
 * @see IOCallbacks#collectMethods(Object)
 * @see IODevice
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IOCallback {
    /**
     * The method code identifying this method to the guest.
     * <p>
     * Must be unique within its declaring class and must be at most {@link #MAX_CODE}.
     * The maximum value is a reserved sentinel, meaning "no method here".
     *
     * @return the method code.
     */
    int value();

    /**
     * The largest value {@link #value()} may take.
     */
    int MAX_CODE = 0xFE;

    /**
     * The method code the register layout reserves; writing it always faults.
     */
    int RESERVED_CODE = 0xFF;

    /**
     * Allows automatically moving method invocation into the main thread.
     * <p>
     * Note that this will lead to dramatically slower method calls as viewed from
     * the caller as each call will take at least one tick (50ms).
     * <p>
     * Use this when the targeted method interacts with data that is not thread
     * safe, for example the level or any objects inside the level, such as
     * entities and block entities.
     *
     * @return {@code true} when to be executed on main thread; {@code false} otherwise.
     */
    boolean synchronize() default true;
}
