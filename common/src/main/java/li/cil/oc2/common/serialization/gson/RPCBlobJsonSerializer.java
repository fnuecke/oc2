/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.*;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class RPCBlobJsonSerializer implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    public static final String BLOB_REFERENCE_KEY = "$blob";

    private byte[][] received;
    private final List<byte[]> pending = new ArrayList<>();
    private final Map<byte[], Integer> pendingIndices = new IdentityHashMap<>();

    // --------------------------------------------------------------------- //

    @Nullable
    public byte[][] getReceived() {
        return received;
    }

    public void setReceived(@Nullable final byte[][] data) {
        received = data;
    }

    public List<byte[]> getPending() {
        return pending;
    }

    public void clearPending() {
        pending.clear();
        pendingIndices.clear();
    }

    // --------------------------------------------------------------------- //

    @Override
    public byte[] deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
        if (!json.isJsonObject() || !json.getAsJsonObject().has(BLOB_REFERENCE_KEY)) {
            throw new JsonParseException("binary parameters must be sent on the payload channel");
        }
        final JsonElement reference = json.getAsJsonObject().get(BLOB_REFERENCE_KEY);
        if (!reference.isJsonPrimitive() || !reference.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("payload reference must be an index");
        }
        final double value = reference.getAsDouble();
        final int index = (int) value;
        if (index != value || received == null || index < 0 || index >= received.length) {
            throw new JsonParseException("parameter refers to a payload that was not sent");
        }
        return received[index];
    }

    @Override
    public JsonElement serialize(final byte[] src, final Type typeOfSrc, final JsonSerializationContext context) {
        final int index = pendingIndices.computeIfAbsent(src, unused -> {
            pending.add(src);
            return pending.size() - 1;
        });

        final JsonObject marker = new JsonObject();
        marker.addProperty(BLOB_REFERENCE_KEY, index);
        return marker;
    }
}
