/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

public final class RPCBlobJsonSerializer implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    public static final String BLOB_REFERENCE_KEY = "$blob";

    private byte[] received;
    private byte[] pending;

    // --------------------------------------------------------------------- //

    @Nullable
    public byte[] getReceived() {
        return received;
    }

    public void setReceived(@Nullable final byte[] data) {
        received = data;
    }

    @Nullable
    public byte[] getPending() {
        return pending;
    }

    public void clearPending() {
        pending = null;
    }

    // --------------------------------------------------------------------- //

    @Override
    public byte[] deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        if (!json.isJsonObject() || !json.getAsJsonObject().has(BLOB_REFERENCE_KEY)) {
            throw new JsonParseException("binary parameters must be sent on the payload channel");
        }
        if (received == null) {
            throw new JsonParseException("parameter refers to a payload that was not sent");
        }
        return received;
    }

    @Override
    public JsonElement serialize(final byte[] src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (pending != null) {
            throw new IllegalStateException("a message may carry at most one binary payload");
        }
        pending = src;

        final JsonObject marker = new JsonObject();
        marker.addProperty(BLOB_REFERENCE_KEY, true);
        return marker;
    }
}
