/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration;

import li.cil.oc2.api.API;
import li.cil.oc2.api.imc.RPCMethodParameterTypeAdapter;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import net.minecraft.Util;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

public final class IMC {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final HashMap<String, Consumer<InterModComms.IMCMessage>> METHODS = Util.make(() -> {
        HashMap<String, Consumer<InterModComms.IMCMessage>> map = new HashMap<>();

        map.put(API.IMC_ADD_RPC_METHOD_PARAMETER_TYPE_ADAPTER, IMC::addRPCMethodParameterTypeAdapter);

        return map;
    });

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(IMC::handleIMCMessages);
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleIMCMessages(final InterModProcessEvent event) {
        event.getIMCStream().forEach(message -> {
            final Consumer<InterModComms.IMCMessage> method = METHODS.get(message.method());
            if (method != null) {
                method.accept(message);
            } else {
                LOGGER.error("Received unknown IMC message [{}] from mod [{}], ignoring.", message.method(), message.senderModId());
            }
        });
    }

    private static void addRPCMethodParameterTypeAdapter(final InterModComms.IMCMessage message) {
        getMessageParameter(message, RPCMethodParameterTypeAdapter.class).ifPresent(value -> {
            try {
                RPCMethodParameterTypeAdapters.addTypeAdapter(value);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("Received invalid type adapter registration [{}] for type [{}] from mod [{}].", value.typeAdapter(), value.type(), message.senderModId());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getMessageParameter(final InterModComms.IMCMessage message, final Class<T> type) {
        final Object value = message.messageSupplier().get();
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        } else {
            LOGGER.error("Received incompatible parameter [{}] for IMC message [{}] from mod [{}]. Expected type is [{}].", message.messageSupplier().get(), message.method(), message.senderModId(), type);
            return Optional.empty();
        }
    }
}
