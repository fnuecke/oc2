package li.cil.oc2.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Optional;

public final class ItemDeviceUtils {
    private static final String ITEM_DEVICE_DATA_TAG_NAME = "item_device";

    ///////////////////////////////////////////////////////////////////

    public static Optional<CompoundNBT> getItemDeviceData(final ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        final CompoundNBT nbt = ItemStackUtils.getModDataTag(stack);
        if (nbt == null) {
            return Optional.empty();
        }

        return Optional.of(nbt.getCompound(ITEM_DEVICE_DATA_TAG_NAME));
    }

    public static void setItemDeviceData(final ItemStack stack, final CompoundNBT data) {
        if (data.isEmpty()) {
            return;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).put(ITEM_DEVICE_DATA_TAG_NAME, data);
    }

    public static Optional<String> getItemDeviceDataKey(@Nullable final IForgeRegistryEntry<?> provider) {
        if (provider == null) {
            return Optional.empty();
        }

        final ResourceLocation providerName = provider.getRegistryName();
        if (providerName == null) {
            return Optional.empty();
        }

        return Optional.of(providerName.toString());
    }
}
