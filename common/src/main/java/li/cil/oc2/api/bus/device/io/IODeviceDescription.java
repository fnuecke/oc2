/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the name a device is known by to a low-level guest, and documents the device.
 * <p>
 * Required on any class carrying {@link IOCallback} methods; collecting throws if it's missing.
 * Unlike the type names of {@link li.cil.oc2.api.bus.device.object.RPCDeviceDescription}, the name must be
 * declared explicitly, because guests read it off the device enumerator to find a device, so
 * it should be short and still unique.
 * <p>
 * This is the mid-level API counterpart of {@link li.cil.oc2.api.bus.device.object.RPCDeviceDescription}.
 *
 * @see IOCallbacks#getName(Object)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IODeviceDescription {
    /**
     * The upper bound on a name's length, matching the names the emulator itself uses.
     */
    int MAX_NAME_LENGTH = 6;

    /**
     * The guest-visible name, at most {@link #MAX_NAME_LENGTH} printable ASCII characters.
     *
     * @return the name of the device.
     */
    String name();

    /**
     * Optional documentation of the device's mid-level API as a whole, such as
     * how values are encoded and other conventions shared by its methods.
     * <p>
     * Markdown; {@code \n} separates lines.
     *
     * @return the description of the device.
     */
    String description() default "";
}
