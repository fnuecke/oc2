package li.cil.oc2.api.bus.device.rpc;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a single method that can be exposed by a {@link RPCDevice}.
 * <p>
 * The easiest and hence recommended way of implementing this interface is to use
 * the {@link ObjectDevice} class.
 * <p>
 * Method parameters are serialized and deserialized using Gson. When using custom
 * parameter types it may be necessary to register a custom type adapter for them
 * via {@link li.cil.oc2.api.API#IMC_ADD_RPC_METHOD_PARAMETER_TYPE_ADAPTER}.
 *
 * @see ObjectDevice
 */
public interface RPCMethod {
    /**
     * The name of the method.
     * <p>
     * When invoked through a {@link DeviceBusController} this is what the method
     * will be referenced by, so the name should be unlikely to be duplicated in
     * another device to avoid ambiguity when devices are combined.
     *
     * @return the name of the method.
     */
    String getName();

    /**
     * When {@code true}, invocations of this method will be synchronized to the main thread.
     *
     * @return {@code true} when to be executed on main thread; {@code false} otherwise.
     */
    boolean isSynchronized();

    /**
     * The type of the values returned by this method.
     *
     * @return the returned type.
     */
    Class<?> getReturnType();

    /**
     * The list of parameters this method accepts.
     *
     * @return the list of parameters.
     */
    RPCParameter[] getParameters();

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
     *
     * @return the method description.
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * An optional description of the return value of this method.
     * <p>
     * May be used inside VMs to generate documentation.
     *
     * @return the return value description.
     */
    default Optional<String> getReturnValueDescription() {
        return Optional.empty();
    }
}
