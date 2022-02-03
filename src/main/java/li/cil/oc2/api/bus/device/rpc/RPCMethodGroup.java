/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import li.cil.oc2.api.bus.DeviceBusController;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * A group of overloaded {@link RPCMethod}s, i.e. methods with the same name but different signatures.
 */
public interface RPCMethodGroup {
    /**
     * The name of the method group/of the grouped {@link RPCMethod}s.
     * <p>
     * When invoked through a {@link DeviceBusController}, this is what the method group
     * will be referenced by, so the name should be unlikely to be duplicated in another
     * device, to avoid ambiguity when devices are combined.
     *
     * @return the name of the method group.
     */
    String getName();

    /**
     * The list of overloads in this method group, if available.
     * <p>
     * This is used when the virtual machine queries the method descriptions for some device, usually
     * to display it to the user as documentation.
     * <p>
     * This may return an empty set, in which case the group will present itself with just its name.
     *
     * @return the set of {@link RPCMethod}s in this method group.
     */
    default Set<RPCMethod> getOverloads() {
        return Collections.emptySet();
    }

    /**
     * Attempts to find an overload in this method group that matches the provided, serialized parameters.
     *
     * @param invocation the invocation information.
     * @return a matching method overload, if possible.
     */
    Optional<RPCMethod> findOverload(RPCInvocation invocation);
}
