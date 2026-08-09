/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.imc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;

import java.lang.reflect.Type;

/**
 * Defines a type adapter implementation that should be used when marshalling
 * parameters of an invocation on an {@link RPCDevice}.
 * <p>
 * Registered adapters will be directly applied to the {@link Gson} instance
 * used for serialization/deserialization of parameters, i.e. the specified
 * {@link #typeAdapter}s should be valid for passing to {@link GsonBuilder#registerTypeAdapter(Type, Object)}.
 */
public record RPCMethodParameterTypeAdapter(Class<?> type, Object typeAdapter) { }
