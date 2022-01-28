package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import li.cil.oc2.api.API;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RPCItemStackTagFilters {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ArrayList<RPCItemStackTagFilter> FILTERS = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

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

    @SubscribeEvent
    public static void handleAddReloadListenerEvent(final AddReloadListenerEvent event) {
        event.addListener(ReloadListener.INSTANCE);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .create();

        public static final ReloadListener INSTANCE = new ReloadListener();

        public ReloadListener() {
            super(GSON, "item_tag_filters");
        }

        @Override
        protected void apply(final Map<ResourceLocation, JsonElement> objects, final ResourceManager resourceManager, final ProfilerFiller profiler) {
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
