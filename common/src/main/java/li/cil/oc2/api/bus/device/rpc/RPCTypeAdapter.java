/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.util.Registries;

import java.lang.reflect.Type;

/**
 * A Gson type adapter for a type used as a parameter or return value of an {@link RPCMethod}.
 * <p>
 * Method parameters and return values cross the virtual machine boundary as JSON, serialized with
 * {@link Gson}. Types that need special handling require an adapter registered with the
 * {@link Registries#RPC_TYPE_ADAPTER} registry.
 *
 * @param type        the type the adapter applies to.
 * @param typeAdapter the adapter, as accepted by {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
 * @see RPCMethod
 * @see Callback
 */
public record RPCTypeAdapter(Class<?> type, Object typeAdapter) {
}
