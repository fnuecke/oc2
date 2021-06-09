package li.cil.oc2.common.serialization.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public final class TextComponentSerializer implements Serializer<ITextComponent> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<ITextComponent> type, final Object value) throws SerializationException {
        final String json = ITextComponent.Serializer.toJson((ITextComponent) value);
        visitor.putObject("value", String.class, json);
    }

    @Nullable
    @Override
    public ITextComponent deserialize(final DeserializationVisitor visitor, final Class<ITextComponent> type, @Nullable final Object value) throws SerializationException {
        if (!visitor.exists("value")) {
            return (ITextComponent) value;
        }

        final String json = (String) visitor.getObject("value", String.class, null);
        return ITextComponent.Serializer.fromJson(json);
    }
}
