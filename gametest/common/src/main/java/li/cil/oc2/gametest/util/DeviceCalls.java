/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOInvocation;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import li.cil.oc2.common.bus.device.util.Devices;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class DeviceCalls {
    public static final int SYSTEM_GET_ITEM_NAME_CODE = 1;
    public static final int SIDE_FRONT = 0;

    // --------------------------------------------------------------------- //

    public static List<Device> devicesAt(final GameTestHelper helper, final BlockPos pos) {
        return Devices.getDevices(Devices.makeQuery(null, helper.getLevel(), helper.absolutePos(pos), null))
            .orElseThrow(() -> new GameTestAssertException("chunk not loaded"))
            .stream()
            .map(info -> info.get().device)
            .toList();
    }

    public static IODevice ioDevice(final Collection<? extends Device> devices, final String name) {
        return devices.stream()
            .filter(device -> device instanceof final IODevice io && name.equals(io.getIOName()))
            .map(IODevice.class::cast)
            .findFirst()
            .orElseThrow(() -> new GameTestAssertException("no " + name + " device"));
    }

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
                method.invoke(ioInvocation(new ByteArrayInputStream(bytes), results));
            } catch (final IllegalArgumentException e) {
                throw e;
            } catch (final Throwable e) {
                throw new GameTestAssertException("function " + code + " failed: " + e);
            }
            return results.toByteArray();
        }

        throw new GameTestAssertException("device has no function with code " + code);
    }

    public static int[] ascii(final String text) {
        final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        final int[] values = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            values[i] = bytes[i] & 0xFF;
        }
        return values;
    }

    public static int[] concat(final int[] head, final String text) {
        final int[] result = Arrays.copyOf(head, head.length + text.length());
        for (int i = 0; i < text.length(); i++) {
            result[head.length + i] = text.charAt(i);
        }
        return result;
    }

    public static int u16(final byte[] bytes) {
        return u16(bytes, 0);
    }

    public static int u16(final byte[] bytes, final int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    public static int i16(final byte[] bytes, final int offset) {
        return (short) u16(bytes, offset);
    }

    public static long u32(final byte[] bytes) {
        long value = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
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

            @Override
            public boolean consumeEnergy(final int amount) {
                return true;
            }
        };
    }

    private static IOInvocation ioInvocation(final InputStream input, final OutputStream output) {
        return new IOInvocation() {
            @Override
            public InputStream getInput() {
                return input;
            }

            @Override
            public OutputStream getOutput() {
                return output;
            }

            @Override
            public boolean consumeEnergy(final int amount) {
                return true;
            }
        };
    }

    private static Optional<RPCMethod> findMethod(final RPCDevice device, final String name, final RPCInvocation invocation) {
        boolean found = false;
        for (final RPCMethodGroup group : device.getMethodGroups()) {
            if (!name.equals(group.getName())) {
                continue;
            }
            found = true;
            final Optional<RPCMethod> overload = group.findOverload(invocation);
            if (overload.isPresent()) {
                return overload;
            }
        }

        if (found) {
            throw new GameTestAssertException("no " + name + " overload matched the arguments");
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

    // --------------------------------------------------------------------- //

    private DeviceCalls() {
    }
}
