/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrarBuilder;
import dev.architectury.registry.registries.RegistrarManager;
import li.cil.oc2.api.API;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;

public abstract class RegistryUtils {
    private enum Phase {
        PRE_INIT,
        INIT,
        POST_INIT,
    }

    private static final List<DeferredRegister<?>> ENTRIES = new ArrayList<>();
    private static Phase phase = Phase.PRE_INIT;

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> RegistrarBuilder<T> builder(final ResourceKey<Registry<T>> key, final T... typeGetter) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        return RegistrarManager.get(API.MOD_ID).builder(key.location(), typeGetter);
    }

    public static <T> DeferredRegister<T> getInitializerFor(final ResourceKey<Registry<T>> key) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        final DeferredRegister<T> entry = DeferredRegister.create(API.MOD_ID, key);
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
            register.register();
        }

        ENTRIES.clear();
    }

    private RegistryUtils() {
    }
}
