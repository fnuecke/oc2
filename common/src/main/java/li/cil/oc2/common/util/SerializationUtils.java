/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class SerializationUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    // --------------------------------------------------------------------- //

    public static CompoundTag saveToTag(final ProblemReporter.PathElement path, final HolderLookup.Provider registries, final Consumer<ValueOutput> saver) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(path, LOGGER)) {
            final TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            saver.accept(output);
            return output.buildResult();
        }
    }

    public static void loadFromTag(final ProblemReporter.PathElement path, final HolderLookup.Provider registries, final CompoundTag tag, final Consumer<ValueInput> loader) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(path, LOGGER)) {
            loader.accept(TagValueInput.create(reporter, registries, tag));
        }
    }

    public static <T extends Enum<T>> void putEnum(final ValueOutput output, final String key, @Nullable final Enum<T> value) {
        if (value != null) {
            output.putInt(key, value.ordinal());
        }
    }

    @Nullable
    public static <T extends Enum<T>> T getEnum(final ValueInput input, final String key, final Class<T> enumType) {
        final int ordinal = input.getIntOr(key, -1);
        final T[] constants = enumType.getEnumConstants();
        return ordinal >= 0 && ordinal < constants.length ? constants[ordinal] : null;
    }

    public static CompoundTag getCompoundOrEmpty(final ValueInput input, final String key) {
        return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
