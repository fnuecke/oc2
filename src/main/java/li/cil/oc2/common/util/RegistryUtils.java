package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class RegistryUtils {
    private enum Phase {
        PRE_INIT,
        INIT,
        POST_INIT,
    }

    private static final List<DeferredRegister<?>> ENTRIES = new ArrayList<>();
    private static Phase phase = Phase.PRE_INIT;

    public static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> create(final Class<T> type) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        final DeferredRegister<T> entry = DeferredRegister.create(type, API.MOD_ID);
        ENTRIES.add(entry);
        return entry;
    }

    public static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> create(final IForgeRegistry<T> registry) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        final DeferredRegister<T> entry = DeferredRegister.create(registry, API.MOD_ID);
        ENTRIES.add(entry);
        return entry;
    }

    public static void begin() {
        if (phase != Phase.PRE_INIT) throw new IllegalStateException();
        phase = Phase.INIT;
    }

    public static void finish() {
        if (phase != Phase.INIT) throw new IllegalStateException();
        phase = Phase.POST_INIT;

        for (final DeferredRegister<?> register : ENTRIES) {
            register.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        ENTRIES.clear();
    }

    public static <T> String key(final IForgeRegistryEntry<T> registryEntry) {
        return Objects.requireNonNull(registryEntry.getRegistryName()).toString();
    }

    private RegistryUtils() {
    }
}
