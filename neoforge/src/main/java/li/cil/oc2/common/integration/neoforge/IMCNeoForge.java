/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.api.imc.RPCMethodParameterTypeAdapter;
import li.cil.oc2.common.bus.device.rpc.RPCMethodParameterTypeAdapters;
import net.minecraft.Util;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = API.MOD_ID)
public final class IMCNeoForge {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final HashMap<String, Consumer<InterModComms.IMCMessage>> METHODS = Util.make(() -> {
        HashMap<String, Consumer<InterModComms.IMCMessage>> map = new HashMap<>();

        map.put(API.IMC_ADD_RPC_METHOD_PARAMETER_TYPE_ADAPTER, IMCNeoForge::addRPCMethodParameterTypeAdapter);

        return map;
    });

    // ------------------------------------------------------------- //

    @SubscribeEvent
    public static void handleIMCMessages(final InterModProcessEvent event) {
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
