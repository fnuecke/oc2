/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.ceres;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import javax.annotation.Nullable;

public final class TextComponentSerializer implements Serializer<Component> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<Component> type, final Object value) throws SerializationException {
        final String json = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, (Component) value).getOrThrow().toString();
        visitor.putObject("value", String.class, json);
    }

    @Nullable
    @Override
    public Component deserialize(final DeserializationVisitor visitor, final Class<Component> type, @Nullable final Object value) throws SerializationException {
        if (!visitor.exists("value")) {
            return (Component) value;
        }

        final String json = (String) visitor.getObject("value", String.class, null);
        if (json == null) {
            return (Component) value;
        }

        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElse(null);
    }
}
