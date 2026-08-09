/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import li.cil.oc2.api.API;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigManager {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Type {
        ModConfig.Type value() default ModConfig.Type.COMMON;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Path {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Min {
        double value() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Max {
        double value() default Double.POSITIVE_INFINITY;
    }

    ///////////////////////////////////////////////////////////////////

    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final Map<Class<?>, ConfigFieldParser> PARSERS = new HashMap<>();
    private static final Map<IConfigSpec<ForgeConfigSpec>, ConfigDefinition> CONFIGS = new HashMap<>();

    static {
        PARSERS.put(int.class, ConfigManager::parseIntField);
        PARSERS.put(long.class, ConfigManager::parseLongField);
        PARSERS.put(double.class, ConfigManager::parseDoubleField);
        PARSERS.put(String.class, ConfigManager::parseStringField);
        PARSERS.put(UUID.class, ConfigManager::parseUUIDField);
        PARSERS.put(ResourceLocation.class, ConfigManager::parseResourceLocationField);
    }

    ///////////////////////////////////////////////////////////////////

    public static <T> void add(final Supplier<T> factory) {
        final ArrayList<ConfigFieldPair<?>> values = new ArrayList<>();
        final Pair<?, ForgeConfigSpec> config = new ForgeConfigSpec.Builder().configure(builder -> {
            final T instance = factory.get();
            fillSpec(instance, builder, values);
            return instance;
        });
        CONFIGS.put(config.getValue(), new ConfigDefinition(config.getKey(), values));
    }

    public static void initialize() {
        CONFIGS.forEach((spec, config) -> {
            final Type typeAnnotation = config.instance.getClass().getAnnotation(Type.class);
            final ModConfig.Type configType = typeAnnotation != null ? typeAnnotation.value() : ModConfig.Type.COMMON;
            ModLoadingContext.get().registerConfig(configType, spec);
        });
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfigEvent event) {
        final ConfigDefinition config = CONFIGS.get(event.getConfig().getSpec());
        if (config != null) {
            config.apply();
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static <T> void fillSpec(final T instance, final ForgeConfigSpec.Builder builder, final ArrayList<ConfigFieldPair<?>> values) {
        for (final Field field : instance.getClass().getFields()) {
            parseField(instance, builder, values, field);
        }
    }

    private static <T> void parseField(final T instance, final ForgeConfigSpec.Builder builder, final ArrayList<ConfigFieldPair<?>> values, final Field field) {
        final ConfigFieldParser parser = PARSERS.get(field.getType());
        if (parser != null) {
            final Path pathAnnotation = field.getAnnotation(Path.class);
            final String path = getPath(pathAnnotation.value(), field);

            try {
                values.add(parser.apply(instance, field, path, builder));
            } catch (final IllegalAccessException e) {
                LOGGER.error("Failed accessing field [{}.{}], ignoring.", field.getDeclaringClass().getName(), field.getName());
            }
        }
    }

    private static ConfigFieldPair<?> parseIntField(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException {
        final int defaultValue = field.getInt(instance);
        final int minValue = (int) Math.max(getMin(field), Integer.MIN_VALUE);
        final int maxValue = (int) Math.min(getMax(field), Integer.MAX_VALUE);

        final ForgeConfigSpec.IntValue configValue = builder.defineInRange(path, defaultValue, minValue, maxValue);

        return new ConfigFieldPair<>(field, configValue);
    }

    private static ConfigFieldPair<?> parseLongField(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException {
        final long defaultValue = field.getLong(instance);
        final long minValue = (long) Math.max(getMin(field), Long.MIN_VALUE);
        final long maxValue = (long) Math.min(getMax(field), Long.MAX_VALUE);

        final ForgeConfigSpec.LongValue configValue = builder.defineInRange(path, defaultValue, minValue, maxValue);

        return new ConfigFieldPair<>(field, configValue);
    }

    private static ConfigFieldPair<?> parseDoubleField(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException {
        final double defaultValue = field.getDouble(instance);
        final double minValue = getMin(field);
        final double maxValue = getMax(field);

        final ForgeConfigSpec.DoubleValue configValue = builder.defineInRange(path, defaultValue, minValue, maxValue);

        return new ConfigFieldPair<>(field, configValue);
    }

    private static ConfigFieldPair<?> parseStringField(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException {
        final String defaultValue = (String) field.get(instance);

        final ForgeConfigSpec.ConfigValue<String> configValue = builder.define(path, defaultValue);

        return new ConfigFieldPair<>(field, configValue);
    }

    private static ConfigFieldPair<?> parseUUIDField(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException {
        final UUID defaultValue = (UUID) field.get(instance);

        final ForgeConfigSpec.ConfigValue<String> configValue = builder.define(path, defaultValue.toString());

        return new ConfigFieldPair<>(field, configValue, UUID::fromString);
    }

    private static ConfigFieldPair<?> parseResourceLocationField(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException {
        final ResourceLocation defaultValue = (ResourceLocation) field.get(instance);

        final ForgeConfigSpec.ConfigValue<String> configValue = builder.define(path, defaultValue.toString());

        return new ConfigFieldPair<>(field, configValue, ResourceLocation::new);
    }

    private static String getPath(@Nullable final String prefix, final Field field) {
        return (prefix != null ? prefix + "." : "") + field.getName();
    }

    private static double getMin(final Field field) {
        final Min minAnnotation = field.getAnnotation(Min.class);
        return minAnnotation != null ? minAnnotation.value() : 0;
    }

    private static double getMax(final Field field) {
        final Max maxAnnotation = field.getAnnotation(Max.class);
        return maxAnnotation != null ? maxAnnotation.value() : Double.POSITIVE_INFINITY;
    }

    ///////////////////////////////////////////////////////////////////

    @FunctionalInterface
    private interface ConfigFieldParser {
        ConfigFieldPair<?> apply(final Object instance, final Field field, final String path, final ForgeConfigSpec.Builder builder) throws IllegalAccessException;
    }

    private record ConfigDefinition(Object instance, ArrayList<ConfigFieldPair<?>> values) {
        public void apply() {
            for (final ConfigFieldPair<?> pair : values) {
                pair.apply(instance);
            }
        }
    }

    private static final class ConfigFieldPair<T> {
        public final Field field;
        public final ForgeConfigSpec.ConfigValue<T> value;
        private final Function<T, Object> converter;

        public ConfigFieldPair(final Field field, final ForgeConfigSpec.ConfigValue<T> value, final Function<T, Object> converter) {
            this.field = field;
            this.value = value;
            this.converter = converter;
        }

        public ConfigFieldPair(final Field field, final ForgeConfigSpec.ConfigValue<T> value) {
            this(field, value, x -> x);
        }

        public void apply(final Object instance) {
            try {
                field.set(instance, converter.apply(value.get()));
            } catch (final IllegalAccessException ignored) {
            }
        }
    }
}
