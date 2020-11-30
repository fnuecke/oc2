package li.cil.oc2.serialization.serializers;

import com.google.gson.*;
import li.cil.oc2.common.bus.DeviceBusControllerImpl;

import java.lang.reflect.Type;

public final class MessageJsonDeserializer implements JsonDeserializer<DeviceBusControllerImpl.Message> {
    @Override
    public DeviceBusControllerImpl.Message deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final String messageType = jsonObject.get("type").getAsString();
        final Object messageData;
        switch (messageType) {
            case DeviceBusControllerImpl.Message.MESSAGE_TYPE_STATUS: {
                messageData = null;
                break;
            }
            case DeviceBusControllerImpl.Message.MESSAGE_TYPE_INVOKE_METHOD: {
                messageData = context.deserialize(jsonObject.getAsJsonObject("data"), DeviceBusControllerImpl.MethodInvocation.class);
                break;
            }
            default: {
                throw new JsonParseException(DeviceBusControllerImpl.ERROR_UNKNOWN_MESSAGE_TYPE);
            }
        }

        return new DeviceBusControllerImpl.Message(messageType, messageData);
    }
}
