/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The name a device is known by to a low-level guest.
 * <p>
 * Required on any class carrying {@link IOCallback} methods; collecting throws if it's missing.
 * Unlike the type names of {@link li.cil.oc2.api.bus.device.object.NamedDevice}, these must be
 * declared explicitly, because guests read it off the device enumerator to find a device, so
 * these should be short and still unique.
 *
 * @see IOCallbacks#getName(Object)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IOName {
    /**
     * The upper bound on a name's length, matching the names the emulator itself uses.
     */
    int MAX_LENGTH = 6;

    /**
     * The guest-visible name, at most {@link #MAX_LENGTH} printable ASCII characters.
     *
     * @return the name of the device.
     */
    String value();
}
