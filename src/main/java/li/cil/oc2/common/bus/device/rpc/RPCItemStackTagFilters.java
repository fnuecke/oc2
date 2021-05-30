package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;

public final class RPCItemStackTagFilters {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ArrayList<RPCItemStackTagFilter> FILTERS = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(RPCItemStackTagFilters::handleAddReloadListenerEvent);
    }

    @Nullable
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

    ///////////////////////////////////////////////////////////////////

    private static void handleAddReloadListenerEvent(final AddReloadListenerEvent event) {
        event.addListener(ReloadListener.INSTANCE);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class ReloadListener extends JsonReloadListener {
        private static final Gson GSON = new GsonBuilder()
                .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
                .create();

        public static final ReloadListener INSTANCE = new ReloadListener();

        public ReloadListener() {
            super(GSON, "item_tag_filters");
        }

        @Override
        protected void apply(final Map<ResourceLocation, JsonElement> objects, final IResourceManager resourceManager, final IProfiler profiler) {
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
