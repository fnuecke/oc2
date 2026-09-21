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
 * {@code ()}, {@code (arguments)}, {@code (results)} or {@code (arguments, results)}. Either may be
 * omitted when unused. As such, the guest must be aware of the device's protocol to use it correctly.
 * <p>
 * Arguments are an {@link IOInputStream} and results an {@link IOOutputStream}, which allow reading
 * and writing values with the appropriate byte order for the backing architecture. Plain {@link java.io.InputStream}
 * and {@link java.io.OutputStream} is also possible, if you only need raw byte access or don't mind
 * taking care of correct endianness yourself.
 * <p>
 * Note that {@link java.io.DataInputStream} and {@link java.io.DataOutputStream} are <em>not</em>
 * right here, since they're explicitly big-endian, which may not match the underlying architecture.
 * For example, the Z80 needs little-endian.
 * <p>
 * The declaring class must have an {@link IODeviceDescription}, since the guest sees devices by name.
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
     * The maximum size of the argument and result buffers, in bytes.
     * <p>
     * Arguments past this are rejected, and a method writing more results than this fails.
     */
    int MAX_DATA_SIZE = 256;

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

    /**
     * The name of this method, for documentation only. Computers call methods by {@link #value()}.
     * If left blank the name of the annotated method will be used.
     *
     * @return the name of the method.
     */
    String name() default "";

    /**
     * Optional documentation of this method.
     *
     * @return the description of the method.
     */
    String description() default "";

    /**
     * Optional documentation of the bytes this method takes as arguments.
     *
     * @return the description of the arguments.
     */
    String argumentsDescription() default "";

    /**
     * Optional documentation of the bytes this method writes as results.
     *
     * @return the description of the results.
     */
    String resultsDescription() default "";
}
