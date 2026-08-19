/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;
import net.minecraft.util.GsonHelper;

import java.lang.reflect.Type;
import java.util.UUID;

public final class MessageJsonDeserializer implements JsonDeserializer<RPCDeviceBusAdapter.Message> {
    @Override
    public RPCDeviceBusAdapter.Message deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final String messageType = jsonObject.get("type").getAsString();
        final Object messageData = switch (messageType) {
            case RPCDeviceBusAdapter.Message.MESSAGE_TYPE_LIST -> null;
            case RPCDeviceBusAdapter.Message.MESSAGE_TYPE_METHODS ->
                    UUID.fromString(jsonObject.getAsJsonPrimitive("data").getAsString());
            case RPCDeviceBusAdapter.Message.MESSAGE_TYPE_INVOKE_METHOD ->
                    context.deserialize(jsonObject.getAsJsonObject("data"), RPCDeviceBusAdapter.MethodInvocation.class);
            default -> throw new JsonParseException(RPCDeviceBusAdapter.ERROR_UNKNOWN_MESSAGE_TYPE);
        };

        int messageId = GsonHelper.getAsInt(jsonObject, "id", 0);
        return new RPCDeviceBusAdapter.Message(messageType, messageId, 0, messageData, null);
    }
}
