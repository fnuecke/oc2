/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import dev.architectury.registry.ReloadListenerRegistry;
import li.cil.oc2.api.API;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;

public final class RPCItemStackTagFilters {
    private static final Logger LOGGER = LogManager.getLogger(RPCItemStackTagFilters.class);
    private static final ArrayList<RPCItemStackTagFilter> FILTERS = new ArrayList<>();

    // --------------------------------------------------------------------- //

    public static CompoundTag getFilteredTag(final ItemStack stack, final CompoundTag tag) {
        final CompoundTag result = new CompoundTag();
        for (final RPCItemStackTagFilter filter : FILTERS) {
            final CompoundTag filtered = filter.apply(stack, tag);
            if (filtered != null) {
                result.merge(filtered);
            }
        }

        return result;
    }

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ReloadListener.INSTANCE,
            Identifier.fromNamespaceAndPath(API.MOD_ID, "item_stack_tag_filters"));
    }

    // --------------------------------------------------------------------- //

    private static final class ReloadListener extends SimpleJsonResourceReloadListener<JsonElement> {
        private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, (JsonDeserializer<Identifier>) (json, type, context) -> Identifier.parse(json.getAsString()))
            .create();

        public static final ReloadListener INSTANCE = new ReloadListener();

        public ReloadListener() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("item_tag_filters"));
        }

        @Override
        protected void apply(final Map<Identifier, JsonElement> objects, final ResourceManager resourceManager, final ProfilerFiller profiler) {
            FILTERS.clear();

            objects.forEach((location, element) -> {
                try {
                    final RPCItemStackTagFilter filter = GSON.fromJson(element, RPCItemStackTagFilter.class);
                    if (filter != null) {
                        FILTERS.add(filter);
                    }
                } catch (final Exception e) {
                    LOGGER.error("Failed loading item tag filter [{}].", location, e);
                }
            });
        }
    }
}
