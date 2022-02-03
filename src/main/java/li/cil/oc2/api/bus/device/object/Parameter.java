/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation may be used to propagate the name of parameters and to
 * provide VM visible documentation of this parameter for methods annotated
 * with the {@link Callback} annotation.
 * <p>
 * Java strips parameter names in non-debug builds, so the actual method
 * parameter names cannot be retrieved directly.
 * <p>
 * If this is not present, parameters will appear unnamed to the VM.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    /**
     * The name of the parameter as seen by the VM.
     *
     * @return the name of the parameter.
     */
    String value();

    /**
     * Optional VM visible documentation of this parameter.
     *
     * @return the description of the parameter.
     */
    String description() default "";
}
