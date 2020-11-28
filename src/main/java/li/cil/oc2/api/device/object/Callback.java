package li.cil.oc2.api.device.object;

import li.cil.oc2.api.device.DeviceMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Utility annotation to allow generating lists of {@link DeviceMethod}s using
 * {@link Callbacks#collectMethods(Object)}.
 * <p>
 * Intended to be used in classes instances of which are used in combination with
 * {@link ObjectDevice} and subclasses of {@link ObjectDevice}.
 * <p>
 * For
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Callback {
    /**
     * Option VM visible documentation of this method.
     */
    String description() default "";

    /**
     * Optional VM visible documentation of the values returned by this method.
     */
    String returnValueDescription() default "";
}
