package li.cil.oc2.api.device;

import li.cil.oc2.api.bus.DeviceBusController;

import java.util.Optional;

/**
 * Describes a single parameter of a {@link DeviceMethod}.
 */
public interface DeviceMethodParameter {
    /**
     * The type of this parameter.
     * <p>
     * This is used by {@link DeviceBusController}s to convert parameters from a lower
     * level representation before passing it to {@link DeviceMethod#invoke(Object...)}.
     * As such, the types used must be kept simple. As a rule of thumb, only primitives
     * and POJOs should be used.
     *
     * @return the type of the parameter.
     */
    Class<?> getType();

    /**
     * An optional name of the parameter.
     * <p>
     * May be used inside VMs to generate documentation.
     */
    default Optional<String> getName() {
        return Optional.empty();
    }

    /**
     * An optional description of the parameter.
     * <p>
     * May be used inside VMs to generate documentation.
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }
}
