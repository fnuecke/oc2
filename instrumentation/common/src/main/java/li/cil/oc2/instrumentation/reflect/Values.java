/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;

import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Values {
    private static final Map<Class<?>, Optional<Codec<?>>> CODECS = new ConcurrentHashMap<>();

    // --------------------------------------------------------------------- //

    public static JsonElement toJson(@Nullable final Object value, final ReflectContext ctx) {
        return encode(value, ctx, 0, Sets.newIdentityHashSet());
    }

    @Nullable
    public static Object fromJson(final String json, final Class<?> type, @Nullable final Type generic, final ReflectContext ctx) {
        try {
            return ctx.gson().fromJson(json, generic != null ? generic : type);
        } catch (final JsonParseException e) {
            throw new IllegalArgumentException(rootMessage(e));
        }
    }

    private static String rootMessage(final Throwable e) {
        final Throwable cause = ExceptionUtils.getRootCause(e);
        return StringUtils.substringBefore(
            cause.getMessage() != null ? cause.getMessage() : e.toString(), " at line ");
    }

    static Gson newGson(final ReflectContext ctx) {
        return new GsonBuilder().serializeNulls().registerTypeAdapterFactory(new Adapters(ctx)).create();
    }

    // --------------------------------------------------------------------- //

    private record Adapters(ReflectContext ctx) implements TypeAdapterFactory {
        @Nullable
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> token) {
            final Class<?> raw = token.getRawType();

            if (raw == String.class) {
                return (TypeAdapter<T>) reader(in -> {
                    final JsonToken peek = in.peek();
                    if (peek != JsonToken.STRING && peek != JsonToken.NAME) {
                        throw new JsonSyntaxException("expected a string, got " + peek);
                    }
                    return in.nextString();
                });
            }
            if (raw == Boolean.class || raw == boolean.class) {
                return (TypeAdapter<T>) reader(in -> {
                    if (in.peek() != JsonToken.BOOLEAN) {
                        throw new JsonSyntaxException("expected a boolean, got " + in.peek());
                    }
                    return in.nextBoolean();
                });
            }
            if (raw.isEnum()) {
                return (TypeAdapter<T>) reader(in -> constant(in.nextString(), raw));
            }
            if (raw == byte[].class) {
                return (TypeAdapter<T>) reader(in -> in.nextString().getBytes(StandardCharsets.UTF_8));
            }
            if (ByteBuffer.class.isAssignableFrom(raw)) {
                return (TypeAdapter<T>) reader(in ->
                    ByteBuffer.wrap(in.nextString().getBytes(StandardCharsets.UTF_8)));
            }
            if (Entity.class.isAssignableFrom(raw)) {
                return (TypeAdapter<T>) reader(in -> entity(in.nextString(), raw, ctx));
            }

            final Optional<Codec<?>> codec = codecFor(raw);
            return codec.map(value -> (TypeAdapter<T>) reader(in ->
                value.parse(ops(ctx), JsonParser.parseReader(in)).getOrThrow(
                    error -> new JsonSyntaxException("codec for " + raw.getSimpleName() + ": " + error)))).orElse(null);
        }
    }

    private record Writers(ReflectContext ctx) implements TypeAdapterFactory {
        @Nullable
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> token) {
            final Class<?> raw = token.getRawType();

            if (Number.class.isAssignableFrom(raw) || Boolean.class.isAssignableFrom(raw)
                || Character.class.isAssignableFrom(raw)) {
                return gson.getDelegateAdapter(this, token);
            }
            if (Enum.class.isAssignableFrom(raw)) {
                return (TypeAdapter<T>) writer(gson, value -> new JsonPrimitive(((Enum<?>) value).name()));
            }
            if (CharSequence.class.isAssignableFrom(raw)) {
                return (TypeAdapter<T>) writer(gson, value -> new JsonPrimitive(value.toString()));
            }
            return (TypeAdapter<T>) codecFor(raw).map(codec -> writer(gson, value -> {
                @SuppressWarnings("unchecked") final Codec<Object> typed = (Codec<Object>) codec;
                return typed.encodeStart(ops(ctx), value).result().orElseGet(
                    () -> new JsonPrimitive("<uncodecable " + raw.getSimpleName() + ">"));
            })).orElse(null);
        }
    }

    private interface Write {
        JsonElement apply(Object value);
    }

    private static TypeAdapter<Object> writer(final Gson gson, final Write write) {
        final TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
        return new TypeAdapter<>() {
            @Override
            public void write(final JsonWriter out, final Object value) throws IOException {
                elements.write(out, write.apply(value));
            }

            @Override
            public Object read(final JsonReader in) {
                throw new UnsupportedOperationException("reading goes through ReflectContext.gson()");
            }
        };
    }

    record WriteSupport(Gson gson, TypeAdapterFactory factory) {
        @Nullable
        @SuppressWarnings("unchecked")
        TypeAdapter<Object> writerFor(final Class<?> type) {
            return (TypeAdapter<Object>) factory.create(gson, TypeToken.get(type));
        }
    }

    static WriteSupport newWriteSupport(final ReflectContext ctx) {
        final TypeAdapterFactory factory = new Writers(ctx);
        return new WriteSupport(new GsonBuilder().registerTypeAdapterFactory(factory).create(), factory);
    }

    private interface Read {
        Object apply(JsonReader in) throws IOException;
    }

    private static TypeAdapter<Object> reader(final Read read) {
        return new TypeAdapter<>() {
            @Override
            public void write(final JsonWriter out, final Object value) {
                throw new UnsupportedOperationException("writing goes through Values.encode");
            }

            @Nullable
            @Override
            public Object read(final JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return read.apply(in);
            }
        };
    }

    private static Object constant(final String name, final Class<?> type) {
        for (final Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                return constant;
            }
        }
         throw new JsonSyntaxException("no constant " + name + " in " + type.getSimpleName());
    }

    private static Object entity(final String selector, final Class<?> type, final ReflectContext ctx) {
        if (ctx.entities == null) {
            throw new JsonSyntaxException("no entity resolver available here");
        }
        final Entity entity = ctx.entities.resolve(selector);
        if (entity == null) {
            throw new JsonSyntaxException("selector matched nothing: " + selector);
        }
        if (!type.isInstance(entity)) {
            throw new JsonSyntaxException(selector + " is a " + entity.getClass().getSimpleName()
                + ", not a " + type.getSimpleName());
        }
        return entity;
    }

    // --------------------------------------------------------------------- //

    private static JsonElement encode(@Nullable final Object value, final ReflectContext ctx, final int depth, final Set<Object> seen) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }

        final Class<?> type = value.getClass();

        final TypeAdapter<Object> adapter = ctx.writer(type);
        if (adapter != null) {
            return adapter.toJsonTree(value);
        }

        if (depth >= ctx.maxDepth) {
            return new JsonPrimitive("<truncated " + type.getSimpleName() + ">");
        }
        if (!seen.add(value)) {
            return new JsonPrimitive("<cycle " + type.getSimpleName() + ">");
        }

        try {
            if (type.isArray()) {
                final JsonArray array = new JsonArray();
                for (int i = 0, n = Array.getLength(value); i < n; i++) {
                    array.add(encode(Array.get(value, i), ctx, depth + 1, seen));
                }
                return array;
            }
            if (value instanceof final Collection<?> collection) {
                final JsonArray array = new JsonArray();
                for (final Object element : collection) {
                    array.add(encode(element, ctx, depth + 1, seen));
                }
                return array;
            }
            if (value instanceof final Map<?, ?> map) {
                final JsonObject object = new JsonObject();
                map.forEach((k, v) -> {
                    String key = String.valueOf(k);
                    for (int i = 2; object.has(key); i++) {
                        key = k + " #" + i;
                    }
                    object.add(key, encode(v, ctx, depth + 1, seen));
                });
                return object;
            }

            final JsonObject object = new JsonObject();
            for (final Field field : FieldUtils.getAllFieldsList(type)) {
                if (Modifier.isStatic(field.getModifiers()) || object.has(field.getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    object.add(field.getName(), encode(field.get(value), ctx, depth + 1, seen));
                } catch (final Throwable e) {
                    object.add(field.getName(), new JsonPrimitive("<unreadable: " + e + ">"));
                }
            }
            return object;
        } finally {
            seen.remove(value);
        }
    }

    static Optional<Codec<?>> codecFor(final Class<?> type) {
        return CODECS.computeIfAbsent(type, t -> {
            if (Component.class.isAssignableFrom(t)) {
                return Optional.of(ComponentSerialization.CODEC);
            }
            for (final Class<?> c : ClassUtils.getAllSuperclasses(t)) {
                final Optional<Codec<?>> inherited = declaredCodec(c, t);
                if (inherited.isPresent()) {
                    return inherited;
                }
            }
            return declaredCodec(t, t);
        });
    }

    private static Optional<Codec<?>> declaredCodec(final Class<?> owner, final Class<?> target) {
        try {
            final Field field = owner.getDeclaredField("CODEC");
            if (!Modifier.isStatic(field.getModifiers()) || !Codec.class.isAssignableFrom(field.getType())) {
                return Optional.empty();
            }
            if (!(field.getGenericType() instanceof final ParameterizedType parameterized)
                || !(parameterized.getActualTypeArguments()[0] instanceof final Class<?> argument)
                || !argument.isAssignableFrom(target)) {
                return Optional.empty();
            }
            field.setAccessible(true);
            return Optional.ofNullable((Codec<?>) field.get(null));
        } catch (final Throwable e) {
            return Optional.empty();
        }
    }

    private static DynamicOps<JsonElement> ops(final ReflectContext ctx) {
        return ctx.registries != null
            ? RegistryOps.create(JsonOps.INSTANCE, ctx.registries)
            : JsonOps.INSTANCE;
    }

    private Values() {
    }
}
