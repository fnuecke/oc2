/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import dev.architectury.injectables.annotations.ExpectPlatform;
import li.cil.oc2.common.config.*;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // ------------------------------------------------------------- //

    private static final Map<Class<?>, ConfigFieldParser> PARSERS = new HashMap<>();
    private static final Map<Class<?>, Pair<Function<Object, String>, Function<String, Object>>> STRING_CONVERTERS = new HashMap<>();

    static {
        PARSERS.put(boolean.class, ConfigManager::parseBooleanField);
        PARSERS.put(int.class, ConfigManager::parseIntField);
        PARSERS.put(long.class, ConfigManager::parseLongField);
        PARSERS.put(double.class, ConfigManager::parseDoubleField);

        STRING_CONVERTERS.put(boolean.class, Pair.of(o -> String.valueOf((boolean) o), Boolean::parseBoolean));
        STRING_CONVERTERS.put(int.class, Pair.of(o -> String.valueOf((int) o), Integer::decode));
        STRING_CONVERTERS.put(long.class, Pair.of(o -> String.valueOf((long) o), Long::decode));
        STRING_CONVERTERS.put(double.class, Pair.of(o -> String.valueOf((double) o), Double::parseDouble));
        STRING_CONVERTERS.put(String.class, Pair.of(s -> (String) s, s -> s));
        STRING_CONVERTERS.put(UUID.class, Pair.of(Object::toString, UUID::fromString));
        STRING_CONVERTERS.put(ResourceLocation.class, Pair.of(Object::toString, ResourceLocation::parse));
    }

    // ------------------------------------------------------------- //

    @ExpectPlatform
    public static <T> void add(final Supplier<T> factory) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void initialize() {
        throw new AssertionError();
    }

    // ------------------------------------------------------------- //

    protected static <T> void fillSpec(final T instance, final Builder builder, final ArrayList<ConfigFieldPair<?>> values) {
        for (final Field field : instance.getClass().getFields()) {
            parseField(instance, builder, values, field);
        }
    }

    protected static ConfigDefinition createDefinition(final Object instance, final ArrayList<ConfigFieldPair<?>> values) {
        return new ConfigDefinition(instance, values);
    }

    // ------------------------------------------------------------- //

    private static <T> void parseField(final T instance, final Builder builder, final ArrayList<ConfigFieldPair<?>> values, final Field field) {
        try {
            if (Collection.class.isAssignableFrom(field.getType())) {
                final ItemType annotation = field.getAnnotation(ItemType.class);
                if (annotation == null) {
                    LOGGER.error("Config field [{}.{}] has a collection type but no @ItemType annotation, ignoring.",
                            field.getDeclaringClass().getName(), field.getName());
                    return;
                }

                parseCollectionField(instance, builder, values, field, annotation);
            } else if (Map.class.isAssignableFrom(field.getType())) {
                final KeyValueTypes annotation = field.getAnnotation(KeyValueTypes.class);
                if (annotation == null) {
                    LOGGER.error("Config field [{}.{}] has a map type but no @KeyValueTypes annotation, ignoring.",
                            field.getDeclaringClass().getName(), field.getName());
                    return;
                }

                parseMapField(instance, builder, values, field, annotation);
            } else {
                parseRegularField(instance, builder, values, field);
            }
        } catch (final IllegalAccessException e) {
            LOGGER.error("Failed accessing field [{}.{}], ignoring.", field.getDeclaringClass().getName(), field.getName());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> void parseCollectionField(final T instance, final Builder builder, final ArrayList<ConfigFieldPair<?>> values, final Field field, final ItemType annotation) throws IllegalAccessException {
        final var serializers = getSerializerPair(instance, annotation.valueSerializer(), STRING_CONVERTERS.get(annotation.value()));
        if (serializers == null) {
            LOGGER.error("Collection item type [{}] is not supported (in field [{}]).", annotation.value(), field);
            return;
        }

        final Collection collection = (Collection) field.get(instance);

        final List<String> serializedValues = new ArrayList<>();
        for (final Object value : collection) {
            final String serializedValue = serializers.getLeft().apply(value);
            if (serializedValue != null) {
                serializedValues.add(serializedValue);
            }
        }

        final var configValue = withCommonAttributes(field, builder).define(getPath(field), serializedValues);

        values.add(new ApplyFieldConfigItem<>(field, configValue, list -> {
            collection.clear();
            for (final String value : list) {
                final Object deserializedValue = serializers.getRight().apply(value);
                if (deserializedValue != null) {
                    collection.add(deserializedValue);
                }
            }
        }));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> void parseMapField(final T instance, final Builder builder, final ArrayList<ConfigFieldPair<?>> values, final Field field, final KeyValueTypes annotation) throws IllegalAccessException {
        final var keySerializers = getSerializerPair(instance, annotation.keySerializer(), STRING_CONVERTERS.get(annotation.keyType()));
        if (keySerializers == null) {
            LOGGER.error("Map key type [{}] is not supported (in field [{}]).", annotation.keyType(), field);
            return;
        }

        final var valueSerializers = getSerializerPair(instance, annotation.valueSerializer(), STRING_CONVERTERS.get(annotation.valueType()));
        if (valueSerializers == null) {
            LOGGER.error("Map value type [{}] is not supported (in field [{}]).", annotation.valueType(), field);
            return;
        }

        final Map map = (Map) field.get(instance);

        final List<String> serializedValues = new ArrayList<>();
        for (final Object rawEntry : map.entrySet()) {
            final Map.Entry entry = (Map.Entry) rawEntry;
            final String serializedKey = keySerializers.getLeft().apply(entry.getKey());
            final String serializedValue = valueSerializers.getLeft().apply(entry.getValue());
            if (serializedKey != null && serializedValue != null) {
                serializedValues.add(serializedKey + "=" + serializedValue);
            }
        }

        final var configValue = withCommonAttributes(field, builder).define(getPath(field), serializedValues);

        values.add(new ApplyFieldConfigItem<>(field, configValue, list -> {
            map.clear();
            for (final String value : list) {
                final String[] parts = value.split("=", 2);
                if (parts.length != 2) {
                    LOGGER.error("Failed parsing setting value [{}].", value);
                    continue;
                }

                final Object deserializedKey = keySerializers.getRight().apply(parts[0]);
                final Object deserializedValue = valueSerializers.getRight().apply(parts[1]);
                if (deserializedKey != null && deserializedValue != null) {
                    map.put(deserializedKey, deserializedValue);
                }
            }
        }));
    }

    private static <T> void parseRegularField(final T instance, final Builder builder, final ArrayList<ConfigFieldPair<?>> values, final Field field) throws IllegalAccessException {
        final ConfigFieldParser parser = PARSERS.get(field.getType());
        if (parser != null) {
            values.add(parser.apply(instance, field, builder));
            return;
        }

        final var serializers = STRING_CONVERTERS.get(field.getType());
        if (serializers != null) {
            values.add(parseStringLikeField(instance, field, builder, serializers));
            return;
        }

        throw new IllegalStateException("Field of type [" + field.getType() + "] is not supported.");
    }

    private static ConfigFieldPair<?> parseBooleanField(final Object instance, final Field field, final Builder builder) throws IllegalAccessException {
        final boolean defaultValue = field.getBoolean(instance);

        return new SetFieldConfigItem<>(field, withCommonAttributes(field, builder).define(getPath(field), defaultValue));
    }

    private static ConfigFieldPair<?> parseIntField(final Object instance, final Field field, final Builder builder) throws IllegalAccessException {
        final int defaultValue = field.getInt(instance);
        final int minValue = (int) Math.max(getMin(field), Integer.MIN_VALUE);
        final int maxValue = (int) Math.min(getMax(field), Integer.MAX_VALUE);

        return new SetFieldConfigItem<>(field, withCommonAttributes(field, builder)
                .defineInRange(getPath(field), defaultValue, minValue, maxValue, Integer.class));
    }

    private static ConfigFieldPair<?> parseLongField(final Object instance, final Field field, final Builder builder) throws IllegalAccessException {
        final long defaultValue = field.getLong(instance);
        final long minValue = (long) Math.max(getMin(field), Long.MIN_VALUE);
        final long maxValue = (long) Math.min(getMax(field), Long.MAX_VALUE);

        return new SetFieldConfigItem<>(field, withCommonAttributes(field, builder)
                .defineInRange(getPath(field), defaultValue, minValue, maxValue, Long.class));
    }

    private static ConfigFieldPair<?> parseDoubleField(final Object instance, final Field field, final Builder builder) throws IllegalAccessException {
        final double defaultValue = field.getDouble(instance);
        final double minValue = getMin(field);
        final double maxValue = getMax(field);

        return new SetFieldConfigItem<>(field, withCommonAttributes(field, builder)
                .defineInRange(getPath(field), defaultValue, minValue, maxValue, Double.class));
    }

    private static ConfigFieldPair<?> parseStringLikeField(final Object instance, final Field field, final Builder builder, final Pair<Function<Object, String>, Function<String, Object>> defaultSerializers) throws IllegalAccessException {
        final var serializers = getSerializerPair(instance, field.getAnnotation(CustomSerializer.class), defaultSerializers);
        if (serializers == null) {
            throw new IllegalStateException("Field of type [" + field.getType() + "] is not supported.");
        }

        final String defaultValue = serializers.getLeft().apply(field.get(instance));

        return new SetFieldConfigItem<>(field, withCommonAttributes(field, builder).define(getPath(field), defaultValue),
                serializers.getRight());
    }

    // ------------------------------------------------------------- //

    private static Builder withCommonAttributes(final Field field, final Builder builder) {
        if (getWorldRestart(field)) {
            builder.worldRestart();
        }

        final String[] comment = getComment(field);
        if (comment.length > 0) {
            builder.comment(comment);
        }

        final String translation = getTranslation(field);
        if (translation != null) {
            builder.translation(translation);
        }

        return builder;
    }

    private static String getPath(final Field field) {
        final Path annotation = field.getAnnotation(Path.class);
        final String prefix = annotation != null ? annotation.value() : "";
        return (prefix.isEmpty() ? "" : prefix + ".") + field.getName();
    }

    private static double getMin(final Field field) {
        final Min annotation = field.getAnnotation(Min.class);
        return annotation != null ? annotation.value() : 0;
    }

    private static double getMax(final Field field) {
        final Max annotation = field.getAnnotation(Max.class);
        return annotation != null ? annotation.value() : Double.POSITIVE_INFINITY;
    }

    private static String[] getComment(final Field field) {
        final Comment annotation = field.getAnnotation(Comment.class);
        return annotation != null ? annotation.value() : new String[0];
    }

    @Nullable
    private static String getTranslation(final Field field) {
        final Translation annotation = field.getAnnotation(Translation.class);
        return annotation != null ? annotation.value() : null;
    }

    private static boolean getWorldRestart(final Field field) {
        return field.getAnnotation(WorldRestart.class) != null
                || field.getDeclaringClass().getAnnotation(WorldRestart.class) != null;
    }

    // ------------------------------------------------------------- //

    @Nullable
    private static Pair<Function<Object, String>, Function<String, Object>> getSerializerPair(
            final Object instance,
            @Nullable final CustomSerializer annotation,
            @Nullable final Pair<Function<Object, String>, Function<String, Object>> defaultSerializers) {
        final Function<Object, String> defaultSerializer = defaultSerializers != null ? defaultSerializers.getLeft() : null;
        final Function<String, Object> defaultDeserializer = defaultSerializers != null ? defaultSerializers.getRight() : null;

        final Function<Object, String> serializer;
        final Function<String, Object> deserializer;
        if (annotation == null) {
            serializer = defaultSerializer;
            deserializer = defaultDeserializer;
        } else {
            serializer = getSerializerMethod(instance, annotation.serializer(), defaultSerializer);
            deserializer = getDeserializerMethod(instance, annotation.deserializer(), defaultDeserializer);
        }

        return serializer != null && deserializer != null ? Pair.of(serializer, deserializer) : null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Function<Object, String> getSerializerMethod(final Object instance, final String methodName, @Nullable final Function<Object, String> defaultSerializer) {
        if (methodName.isEmpty()) {
            return defaultSerializer;
        }

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final MethodType methodType = MethodType.methodType(String.class, Object.class);
            final MethodHandle methodHandle = lookup.findStatic(instance.getClass(), methodName, methodType);
            return (Function<Object, String>) LambdaMetafactory.metafactory(lookup, "apply",
                    MethodType.methodType(Function.class), methodType.generic(), methodHandle, methodType).getTarget().invokeExact();
        } catch (final Throwable e) {
            LOGGER.error("Serializer [{}] not found on config [{}] or could not be accessed. Error was: {}", methodName, instance.getClass().getName(), e);
            warnAboutSignature(instance, methodName, "static String {}(Object) {...}");
            return defaultSerializer;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static Function<String, Object> getDeserializerMethod(final Object instance, final String methodName, @Nullable final Function<String, Object> defaultDeserializer) {
        if (methodName.isEmpty()) {
            return defaultDeserializer;
        }

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final MethodType methodType = MethodType.methodType(Object.class, String.class);
            final MethodHandle methodHandle = lookup.findStatic(instance.getClass(), methodName, methodType);
            return (Function<String, Object>) LambdaMetafactory.metafactory(lookup, "apply",
                    MethodType.methodType(Function.class), methodType.generic(), methodHandle, methodType).getTarget().invokeExact();
        } catch (final Throwable e) {
            LOGGER.error("Deserializer [{}] not found on config [{}] or could not be accessed. Error was: {}", methodName, instance.getClass().getName(), e);
            warnAboutSignature(instance, methodName, "static Object {}(String) {...}");
            return defaultDeserializer;
        }
    }

    private static void warnAboutSignature(final Object instance, final String methodName, final String expected) {
        for (final Method method : instance.getClass().getDeclaredMethods()) {
            if (Objects.equals(method.getName(), methodName)) {
                LOGGER.error("A method with this name exists but has an incompatible signature. Signature should be [" + expected + "].", methodName);
                break;
            }
        }
    }

    // ------------------------------------------------------------- //

    @FunctionalInterface
    private interface ConfigFieldParser {
        ConfigFieldPair<?> apply(final Object instance, final Field field, final Builder builder) throws IllegalAccessException;
    }

    protected record ConfigDefinition(Object instance, ArrayList<ConfigFieldPair<?>> values) {
        public void apply() {
            for (final ConfigFieldPair<?> pair : values) {
                pair.apply(instance);
            }
        }

        public void applyDefaults() {
            for (final ConfigFieldPair<?> pair : values) {
                pair.applyDefault(instance);
            }
        }
    }

    protected abstract static class ConfigFieldPair<T> {
        public final Field field;
        public final ConfigValue<T> value;

        public ConfigFieldPair(final Field field, final ConfigValue<T> value) {
            this.field = field;
            this.value = value;
        }

        public abstract void apply(Object instance);

        public abstract void applyDefault(Object instance);
    }

    private static final class SetFieldConfigItem<T> extends ConfigFieldPair<T> {
        private final Function<T, Object> converter;

        public SetFieldConfigItem(final Field field, final ConfigValue<T> value, final Function<T, Object> converter) {
            super(field, value);
            this.converter = converter;
        }

        public SetFieldConfigItem(final Field field, final ConfigValue<T> value) {
            this(field, value, x -> x);
        }

        @Override
        public void apply(final Object instance) {
            set(instance, value.get());
        }

        @Override
        public void applyDefault(final Object instance) {
            set(instance, value.getDefault());
        }

        private void set(final Object instance, final T raw) {
            try {
                field.set(instance, converter.apply(raw));
            } catch (final IllegalAccessException e) {
                LOGGER.error("Failed setting config field [{}.{}].",
                        field.getDeclaringClass().getName(), field.getName(), e);
            } catch (final RuntimeException e) {
                LOGGER.error("Invalid value for config field [{}.{}], keeping previous value.",
                        field.getDeclaringClass().getName(), field.getName(), e);
            }
        }
    }

    private static final class ApplyFieldConfigItem<T> extends ConfigFieldPair<T> {
        private final Consumer<T> applier;

        public ApplyFieldConfigItem(final Field field, final ConfigValue<T> value, final Consumer<T> applier) {
            super(field, value);
            this.applier = applier;
        }

        @Override
        public void apply(final Object instance) {
            applier.accept(value.get());
        }

        @Override
        public void applyDefault(final Object instance) {
            applier.accept(value.getDefault());
        }
    }

    protected interface Builder {
        <T> ConfigValue<T> define(String path, T defaultValue);

        <T extends Comparable<? super T>> ConfigValue<T> defineInRange(String path, T defaultValue, T min, T max, Class<T> type);

        Builder comment(String... comment);

        Builder translation(@Nullable String translationKey);

        Builder worldRestart();
    }

    protected interface ConfigValue<T> {
        T get();

        T getDefault();
    }
}
