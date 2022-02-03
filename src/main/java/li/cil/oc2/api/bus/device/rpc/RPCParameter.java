/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import java.util.Optional;

/**
 * Describes a single parameter of a {@link RPCMethod}.
 */
public interface RPCParameter {
    /**
     * The type of this parameter.
     * <p>
     * May be used inside VMs to generate documentation.
     * <p>
     * This is used by {@link AbstractRPCMethod}s to convert parameters from a lower
     * level representation before passing it to {@link AbstractRPCMethod#invoke(Object...)}.
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
     *
     * @return the name of the parameter.
     */
    default Optional<String> getName() {
        return Optional.empty();
    }

    /**
     * An optional description of the parameter.
     * <p>
     * May be used inside VMs to generate documentation.
     *
     * @return the description of the parameter.
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }
}
