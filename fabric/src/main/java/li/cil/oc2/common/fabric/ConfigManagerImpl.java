/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import li.cil.oc2.api.API;
import li.cil.oc2.common.ConfigManager;
import li.cil.oc2.common.config.ConfigType;
import li.cil.oc2.common.config.Type;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ConfigManagerImpl extends ConfigManager {
    private static final Map<IConfigSpec, ConfigDefinition> CONFIGS = new HashMap<>();

    // ------------------------------------------------------------- //

    public static <T> void add(final Supplier<T> factory) {
        final ArrayList<ConfigFieldPair<?>> values = new ArrayList<>();
        final var config = new ModConfigSpec.Builder().configure(builder -> {
            final T instance = factory.get();
            fillSpec(instance, new BuilderImpl(builder), values);
            return instance;
        });
        CONFIGS.put(config.getValue(), createDefinition(config.getKey(), values));
    }

    public static void initialize() {
        CONFIGS.forEach((spec, config) -> {
            final Type typeAnnotation = config.instance().getClass().getAnnotation(Type.class);
            final ConfigType configType = typeAnnotation != null ? typeAnnotation.value() : ConfigType.COMMON;
            NeoForgeConfigRegistry.INSTANCE.register(API.MOD_ID, switch (configType) {
                case COMMON -> ModConfig.Type.COMMON;
                case CLIENT -> ModConfig.Type.CLIENT;
                case SERVER -> ModConfig.Type.SERVER;
            }, spec);
        });

        NeoForgeModConfigEvents.loading(API.MOD_ID).register(config -> apply(config, false));
        NeoForgeModConfigEvents.reloading(API.MOD_ID).register(config -> apply(config, false));
        NeoForgeModConfigEvents.unloading(API.MOD_ID).register(config -> apply(config, true));
    }

    // ------------------------------------------------------------- //

    private static void apply(final ModConfig modConfig, final boolean isUnloading) {
        final ConfigDefinition config = CONFIGS.get(modConfig.getSpec());
        if (config == null) {
            return;
        }

        if (isUnloading) {
            // The values are gone by now, and reading one throws. Fall back to what we shipped with, so we
            // neither blow up here nor keep serving the settings of a server we have just left.
            config.applyDefaults();
        } else {
            config.apply();
        }
    }

    // ------------------------------------------------------------- //

    private record BuilderImpl(ModConfigSpec.Builder builder) implements Builder {
        @Override
        public <T> ConfigValue<T> define(final String path, final T defaultValue) {
            return new ConfigValueImpl<>(builder.define(path, defaultValue));
        }

        @Override
        public <T extends Comparable<? super T>> ConfigValue<T> defineInRange(final String path, final T defaultValue, final T min, final T max, final Class<T> type) {
            return new ConfigValueImpl<>(builder.defineInRange(path, defaultValue, min, max, type));
        }

        @Override
        public Builder comment(final String... comment) {
            builder.comment(comment);
            return this;
        }

        @Override
        public Builder translation(final String translationKey) {
            builder.translation(translationKey);
            return this;
        }

        @Override
        public Builder worldRestart() {
            builder.worldRestart();
            return this;
        }
    }

    private record ConfigValueImpl<T>(ModConfigSpec.ConfigValue<T> value) implements ConfigValue<T> {
        @Override
        public T get() {
            return value().get();
        }

        @Override
        public T getDefault() {
            return value().getDefault();
        }
    }
}
