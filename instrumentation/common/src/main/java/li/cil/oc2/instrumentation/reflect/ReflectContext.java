/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class ReflectContext {
    public interface EntityResolver {
        @Nullable
        Entity resolve(String selector);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public final HolderLookup.Provider registries;

    @Nullable
    public final EntityResolver entities;

    public final int maxDepth;

    public final Consumer<Object> onVisit;

    @Nullable
    private Gson gson;

    @Nullable
    private Values.WriteSupport writeSupport;

    // --------------------------------------------------------------------- //

    public ReflectContext(@Nullable final HolderLookup.Provider registries,
                          @Nullable final EntityResolver entities,
                          final int maxDepth) {
        this(registries, entities, maxDepth, value -> {
        });
    }

    public ReflectContext(@Nullable final HolderLookup.Provider registries,
                          @Nullable final EntityResolver entities,
                          final int maxDepth,
                          final Consumer<Object> onVisit) {
        this.registries = registries;
        this.entities = entities;
        this.maxDepth = maxDepth;
        this.onVisit = onVisit;
    }

    Gson gson() {
        if (gson == null) {
            gson = Values.newGson(this);
        }
        return gson;
    }

    @Nullable
    TypeAdapter<Object> writer(final Class<?> type) {
        if (writeSupport == null) {
            writeSupport = Values.newWriteSupport(this);
        }
        return writeSupport.writerFor(type);
    }
}
