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
 * Intended to be used in classes instances of which are used as a target of {@link ObjectDeviceInterface}.
 * <p>
 * Method parameters are serialized and deserialized using Gson. When using custom
 * parameter types it may be necessary to register a custom type adapter for them
 * via {@link li.cil.oc2.api.API#IMC_ADD_DEVICE_METHOD_PARAMETER_TYPE_ADAPTER}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Callback {
    /**
     * Allows automatically moving method invocation into the main thread.
     * <p>
     * Note that this will lead to dramatically slower method calls as viewed from
     * the caller as each call will take at least one tick (50ms).
     * <p>
     * Use this when the targeted method interacts with data that is not thread
     * safe, for example the world or any objects inside the world, such as
     * tile entities and entities.
     */
    boolean synchronize() default true;

    /**
     * Option VM visible documentation of this method.
     */
    String description() default "";

    /**
     * Optional VM visible documentation of the values returned by this method.
     */
    String returnValueDescription() default "";
}
