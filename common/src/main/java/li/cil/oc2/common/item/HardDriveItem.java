/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import com.google.common.annotations.VisibleForTesting;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import net.minecraft.IdentifierException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import javax.annotation.Nullable;
import java.util.function.IntSupplier;

public final class HardDriveItem extends AbstractStorageItem implements ColoredItem, CreativeTabItemProvider {
    private final int defaultColor;

    // --------------------------------------------------------------------- //

    public HardDriveItem(final Properties properties, final IntSupplier defaultCapacity, final DyeColor defaultColor) {
        super(properties.overrideDescription(Util.makeDescriptionId("item", Identifier.fromNamespaceAndPath(API.MOD_ID, "hard_drive"))), defaultCapacity);
        this.defaultColor = defaultColor.getTextureDiffuseColor();
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getStringOr(StorageItemUtils.IMAGE_TAG_NAME, "");
        if (StringUtil.isNullOrEmpty(registryName)) {
            return null;
        }

        try {
            return BlockDeviceDataRegistry.getValue(Identifier.parse(registryName));
        } catch (final IdentifierException ignored) {
            return null;
        }
    }

    public ItemStack withData(final ItemStack stack, final Identifier key) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return ItemStack.EMPTY;
        }

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putString(StorageItemUtils.IMAGE_TAG_NAME, key.toString()));

        return stack;
    }

    public ItemStack withData(final Identifier key) {
        return withData(new ItemStack(this), key);
    }

    @VisibleForTesting
    public boolean isSmallestDriveFor(final long size) {
        HardDriveItem best = null;
        for (final var supplier : Items.HARD_DRIVES) {
            best = supplier.get();
            if (size <= best.getCapacity(new ItemStack(best))) {
                break;
            }
        }
        return best == this;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void addCreativeTabItems(final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output) {
        output.accept(new ItemStack(this));

        BlockDeviceDataRegistry.hardDriveValues().forEach(data -> {
            final Identifier key = BlockDeviceDataRegistry.getKey(data);
            if (key == null || !isSmallestDriveFor(data.getCapacity())) {
                return;
            }

            final ItemStack stack = withData(key);
            final DyeColor color = data.getColor();
            if (color != null) {
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor()));
            }
            output.accept(stack);
        });
    }

    @Override
    public int getColor(final ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, defaultColor);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final BlockDeviceData data = getData(stack);
        if (data == null) {
            return super.getName(stack);
        }

        return Component.literal("")
            .append(super.getName(stack))
            .append(" (")
            .append(data.getDisplayName())
            .append(")");
    }

    // --------------------------------------------------------------------- //

    @Override
    protected int getImageCapacity(final ItemStack stack) {
        final BlockDeviceData data = getData(stack);
        return data != null ? (int) data.getCapacity() : 0;
    }
}
