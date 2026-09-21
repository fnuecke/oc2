/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names and documents the high-level API of a class carrying {@link Callback} methods.
 * <p>
 * Optional. Without type names, one is derived from the class name, see {@link Callbacks#getTypeNames(Object)}.
 * <p>
 * This is the high-level API counterpart of {@link li.cil.oc2.api.bus.device.io.IODeviceDescription}.
 *
 * @see Callbacks#getTypeNames(Object)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCDeviceDescription {
    /**
     * The type names a guest can find the device by.
     * <p>
     * In a more general sense, these are tags the device can be referenced by inside a VM.
     *
     * @return the type names of the device.
     */
    String[] typeNames() default {};

    /**
     * Optional documentation of the device's high-level API as a whole, such as what the
     * device is, how to obtain it from a guest, and conventions shared by its methods.
     * <p>
     * Markdown; {@code \n} separates lines.
     *
     * @return the description of the device.
     */
    String description() default "";
}
