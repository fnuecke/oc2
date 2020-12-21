package li.cil.oc2.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ItemDeviceUtils {
    private static final String ITEM_DEVICE_DATA_NBT_TAG_NAME = "item_device";

    ///////////////////////////////////////////////////////////////////

    public static Optional<CompoundTag> getItemDeviceData(final ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        final CompoundTag nbt = ItemStackUtils.getModDataTag(stack);
        if (nbt == null) {
            return Optional.empty();
        }

        return Optional.of(nbt.getCompound(ITEM_DEVICE_DATA_NBT_TAG_NAME));
    }

    public static void setItemDeviceData(final ItemStack stack, final CompoundTag data) {
        if (data.isEmpty()) {
            return;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).put(ITEM_DEVICE_DATA_NBT_TAG_NAME, data);
    }

    public static <T> Optional<String> getItemDeviceDataKey(final Registry<T> registry, @Nullable final T provider) {
        if (provider == null) {
            return Optional.empty();
        }

        final Identifier providerName = registry.getId(provider);
        if (providerName == null) {
            return Optional.empty();
        }

        return Optional.of(providerName.toString());
    }
}
