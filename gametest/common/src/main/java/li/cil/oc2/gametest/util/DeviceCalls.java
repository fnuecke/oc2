/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import net.minecraft.gametest.framework.GameTestAssertException;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

public final class DeviceCalls {
    @Nullable
    public static Object invokeRpc(final RPCDevice device, final String name, final Object... arguments) {
        final RPCInvocation invocation = invocation(arguments);
        return invoke(findMethod(device, name, invocation)
            .orElseThrow(() -> new GameTestAssertException("the device exposes no " + name + " callback")), name, invocation);
    }

    @Nullable
    public static Object invokeRpc(final Iterable<? extends Device> devices, final String name, final Object... arguments) {
        final RPCInvocation invocation = invocation(arguments);
        for (final Device device : devices) {
            if (device instanceof final RPCDevice rpcDevice) {
                final Optional<RPCMethod> method = findMethod(rpcDevice, name, invocation);
                if (method.isPresent()) {
                    return invoke(method.get(), name, invocation);
                }
            }
        }

        throw new GameTestAssertException("no device exposes a " + name + " callback");
    }

    public static byte[] invokeIo(final IODevice device, final int code, final int... arguments) {
        final byte[] bytes = new byte[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            bytes[i] = (byte) arguments[i];
        }

        for (final IOMethod method : device.getIOMethods()) {
            if (method.getCode() != code) {
                continue;
            }
            final ByteArrayOutputStream results = new ByteArrayOutputStream();
            try {
                method.invoke(new ByteArrayInputStream(bytes), results);
            } catch (final IllegalArgumentException e) {
                throw e;
            } catch (final Throwable e) {
                throw new GameTestAssertException("function " + code + " failed: " + e);
            }
            return results.toByteArray();
        }

        throw new GameTestAssertException("device has no function with code " + code);
    }

    // --------------------------------------------------------------------- //

    private static RPCInvocation invocation(final Object... arguments) {
        final Gson gson = RPCTypeAdapters.beginBuildGson().create();
        final JsonArray parameters = new JsonArray();
        for (final Object argument : arguments) {
            parameters.add(gson.toJsonTree(argument));
        }

        return new RPCInvocation() {
            @Override
            public JsonArray getParameters() {
                return parameters;
            }

            @Override
            public Gson getGson() {
                return gson;
            }

            @Override
            public Optional<Object[]> tryDeserializeParameters(final RPCParameter... parameterTypes) {
                if (parameterTypes.length != parameters.size()) {
                    return Optional.empty();
                }

                final Object[] result = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    try {
                        result[i] = gson.fromJson(parameters.get(i), parameterTypes[i].getType());
                    } catch (final Throwable e) {
                        return Optional.empty();
                    }
                }
                return Optional.of(result);
            }
        };
    }

    private static Optional<RPCMethod> findMethod(final RPCDevice device, final String name, final RPCInvocation invocation) {
        for (final RPCMethodGroup group : device.getMethodGroups()) {
            if (!name.equals(group.getName())) {
                continue;
            }
            return Optional.of(group.findOverload(invocation)
                .orElseThrow(() -> new GameTestAssertException("no " + name + " overload matched the arguments")));
        }

        return Optional.empty();
    }

    @Nullable
    private static Object invoke(final RPCMethod method, final String name, final RPCInvocation invocation) {
        try {
            return method.invoke(invocation);
        } catch (final Throwable e) {
            throw new GameTestAssertException(name + " threw: " + e);
        }
    }

    private DeviceCalls() {
    }
}
