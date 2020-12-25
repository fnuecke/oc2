package li.cil.oc2.common.serialization.serializers;

import com.google.gson.*;
import li.cil.oc2.common.bus.RPCAdapter;

import java.lang.reflect.Type;
import java.util.UUID;

public final class MessageJsonDeserializer implements JsonDeserializer<RPCAdapter.Message> {
    @Override
    public RPCAdapter.Message deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final String messageType = jsonObject.get("type").getAsString();
        final Object messageData;
        switch (messageType) {
            case RPCAdapter.Message.MESSAGE_TYPE_LIST: {
                messageData = null;
                break;
            }
            case RPCAdapter.Message.MESSAGE_TYPE_METHODS: {
                messageData = UUID.fromString(jsonObject.getAsJsonPrimitive("data").getAsString());
                break;
            }
            case RPCAdapter.Message.MESSAGE_TYPE_INVOKE_METHOD: {
                messageData = context.deserialize(jsonObject.getAsJsonObject("data"), RPCAdapter.MethodInvocation.class);
                break;
            }
            default: {
                throw new JsonParseException(RPCAdapter.ERROR_UNKNOWN_MESSAGE_TYPE);
            }
        }

        return new RPCAdapter.Message(messageType, messageData);
    }
}
