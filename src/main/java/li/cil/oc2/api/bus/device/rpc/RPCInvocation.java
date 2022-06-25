/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.Optional;

/**
 * Describes an invocation context for an {@link RPCMethod}.
 * <p>
 * This is used to both query {@link RPCMethodGroup}s for matching overloads, as
 * well as for invoking matched {@link RPCMethod}s.
 */
public interface RPCInvocation {
    /**
     * The raw parameter list of this invocation.
     *
     * @return the parameter list.
     */
    JsonArray getParameters();

    /**
     * The serialization context that may be used to deserialize parameters.
     *
     * @return the serialization context.
     */
    Gson getGson();

    /**
     * Utility method for deserializing parameters, given a list of parameter types.
     * <p>
     * This method softly fails by returning {@link Optional#empty()} if:
     * <ul>
     * <li>The length of the parameters in this invocation and the length of the parameter types do not match.</li>
     * <li>Any one of the parameters cannot be deserialized into the required type.</li>
     * </ul>
     *
     * @param parameterTypes the parameter types to deserialize into.
     * @return the deserialized parameters, if possible.
     */
    Optional<Object[]> tryDeserializeParameters(RPCParameter... parameterTypes);
}
