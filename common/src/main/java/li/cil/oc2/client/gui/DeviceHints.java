/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.ReloadListenerRegistry;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.*;

@Environment(EnvType.CLIENT)
public final class DeviceHints {
    public record Hint(Optional<ItemPredicate> item, boolean isRequired) {
    }

    // --------------------------------------------------------------------- //

    private static final String DIRECTORY = "device_hints";

    @Nullable
    private static DeviceHints cached;
    @Nullable
    private static RegistryAccess cachedRegistries;

    // --------------------------------------------------------------------- //

    private final List<Rule> rules;

    // --------------------------------------------------------------------- //

    private DeviceHints(final List<Rule> rules) {
        this.rules = rules;
    }

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, (ResourceManagerReloadListener) manager -> cached = null,
            Identifier.fromNamespaceAndPath(API.MOD_ID, DIRECTORY));
    }

    public static DeviceHints get() {
        final RegistryAccess registries = Objects.requireNonNull(Minecraft.getInstance().getConnection()).registryAccess();
        if (cached == null || !registries.equals(cachedRegistries)) {
            cached = load(registries);
            cachedRegistries = registries;
        }
        return cached;
    }

    public List<Hint> get(final DeviceType type) {
        final List<Hint> hints = new ArrayList<>();
        for (final Rule rule : rules) {
            if (rule.requires().contains(type.getTag())) {
                hints.add(new Hint(rule.item(), true));
            }
            if (rule.suggests().contains(type.getTag())) {
                hints.add(new Hint(rule.item(), false));
            }
        }
        hints.sort(Comparator.comparing(hint -> !hint.isRequired()));
        return hints;
    }

    // --------------------------------------------------------------------- //

    private static DeviceHints load(final RegistryAccess registries) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        final Map<Identifier, Rule> rules = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(Minecraft.getInstance().getResourceManager(), FileToIdConverter.json(DIRECTORY), ops, Rule.CODEC, rules);
        return new DeviceHints(List.copyOf(rules.values()));
    }

    // --------------------------------------------------------------------- //

    private record Rule(Optional<ItemPredicate> item, List<TagKey<Item>> requires, List<TagKey<Item>> suggests) {
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemPredicate.CODEC.optionalFieldOf("item").forGetter(Rule::item),
            TagKey.hashedCodec(Registries.ITEM).listOf().optionalFieldOf("requires", List.of()).forGetter(Rule::requires),
            TagKey.hashedCodec(Registries.ITEM).listOf().optionalFieldOf("suggests", List.of()).forGetter(Rule::suggests)
        ).apply(instance, Rule::new));
    }
}
