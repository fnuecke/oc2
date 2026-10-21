/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import dev.architectury.event.events.common.PlayerEvent;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import li.cil.oc2.api.API;
import li.cil.oc2.common.ConfigManager;
import li.cil.oc2.common.config.ConfigType;
import li.cil.oc2.common.config.Type;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.ServerConfigMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ConfigManagerImpl extends ConfigManager {
    private static final Map<IConfigSpec, ConfigDefinition> CONFIGS = new HashMap<>();

    // --------------------------------------------------------------------- //

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
        NeoForgeModConfigEvents.loading(API.MOD_ID).register(config -> handleModConfigEvent(config, false));
        NeoForgeModConfigEvents.reloading(API.MOD_ID).register(config -> handleModConfigEvent(config, false));
        NeoForgeModConfigEvents.unloading(API.MOD_ID).register(config -> handleModConfigEvent(config, true));
        PlayerEvent.PLAYER_JOIN.register(ConfigManagerImpl::sendServerConfigs);

        CONFIGS.forEach((spec, config) -> {
            final Type typeAnnotation = config.instance().getClass().getAnnotation(Type.class);
            final ConfigType configType = typeAnnotation != null ? typeAnnotation.value() : ConfigType.COMMON;
            final ModConfig.Type platformType = switch (configType) {
                case COMMON -> ModConfig.Type.COMMON;
                case CLIENT -> ModConfig.Type.CLIENT;
                case SERVER -> ModConfig.Type.SERVER;
            };
            NeoForgeConfigRegistry.INSTANCE.register(API.MOD_ID, platformType, spec);
        });
    }

    public static void applyConfigFile(final String fileName, final byte[] contents) {
        ConfigTracker.acceptSyncedConfig(ModConfigs.getFileMap().get(fileName), contents);
    }

    // --------------------------------------------------------------------- //

    private static void sendServerConfigs(final ServerPlayer player) {
        for (final ModConfig config : ModConfigs.getModConfigs(API.MOD_ID)) {
            if (config.getType() == ModConfig.Type.SERVER) {
                try {
                    Network.sendToClient(new ServerConfigMessage(config.getFileName(), Files.readAllBytes(config.getFullPath())), player);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private static void handleModConfigEvent(final ModConfig eventConfig, final boolean isUnloading) {
        final ConfigDefinition config = CONFIGS.get(eventConfig.getSpec());
        if (config == null) {
            return;
        }

        if (isUnloading) {
            config.applyDefaults();
        } else {
            config.apply();
        }
    }

    // --------------------------------------------------------------------- //

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
