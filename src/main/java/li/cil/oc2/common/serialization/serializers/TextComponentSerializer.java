package li.cil.oc2.common.serialization.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public final class TextComponentSerializer implements Serializer<Component> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<Component> type, final Object value) throws SerializationException {
        final String json = Component.Serializer.toJson((Component) value);
        visitor.putObject("value", String.class, json);
    }

    @Nullable
    @Override
    public Component deserialize(final DeserializationVisitor visitor, final Class<Component> type, @Nullable final Object value) throws SerializationException {
        if (!visitor.exists("value")) {
            return (Component) value;
        }

        final String json = (String) visitor.getObject("value", String.class, null);
        return Component.Serializer.fromJson(json);
    }
}
