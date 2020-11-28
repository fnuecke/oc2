package li.cil.oc2.api.device;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.device.object.ObjectDevice;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Represents a single method that can be exposed by a {@link Device}.
 * <p>
 * The easiest and hence recommended way of implementing this interface is to use
 * the {@link ObjectDevice} class.
 *
 * @see ObjectDevice
 */
public interface DeviceMethod {
    /**
     * The name of the method.
     * <p>
     * When invoked through a {@link DeviceBusController} this is what the method
     * will be referenced by, so the name should be unlikely to be duplicated in
     * another device to avoid ambiguity when devices are combined, e.g. in a
     * {@link CompoundDevice}.
     */
    String getName();

    /**
     * The type of the values returned by this method.
     */
    Class<?> getReturnType();

    /**
     * The list of parameters this method accepts.
     */
    DeviceMethodParameter[] getParameters();

    /**
     * Called to run this method.
     * <p>
     * Implementations should expect the passed {@code parameters} to match the
     * declared parameters returned by {@link #getParameters()}. If the parameters
     * do not match, and exception should be raised.
     * <p>
     * <b>Important:</b> methods are expected to not irrevocably corrupt internal
     * state, even when they throw an exception. As such, implementations should
     * perform internal error handling to prevent state corruption and only throw
     * exceptions to communicate that an error happened during the invocation.
     *
     * @param parameters the parameters for the method.
     * @return the return value, or {@code null} if none.
     * @throws Throwable if the parameters did not match or something inside the
     *                   method caused an exception. The caller is responsible for
     *                   catching these and passing them on appropriately.
     */
    @Nullable
    Object invoke(Object... parameters) throws Throwable;

    /**
     * An optional description of the method.
     * <p>
     * May be used inside VMs to generate documentation.
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * An optional description of the return value of this method.
     * <p>
     * May be used inside VMs to generate documentation.
     */
    default Optional<String> getReturnValueDescription() {
        return Optional.empty();
    }
}
