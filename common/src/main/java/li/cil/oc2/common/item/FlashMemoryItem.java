/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import javax.annotation.Nullable;

public final class FlashMemoryItem extends AbstractStorageItem implements ColoredItem, CreativeTabItemProvider {
    private static final int BLANK_COLOR = 0xFF77B294; // green

    // --------------------------------------------------------------------- //

    @Nullable
    private String descriptionId;

    public FlashMemoryItem(final int defaultCapacity) {
        super(createProperties().stacksTo(1), defaultCapacity);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(StorageItemUtils.IMAGE_TAG_NAME);
        if (StringUtil.isNullOrEmpty(registryName)) {
            return null;
        }

        try {
            return BlockDeviceDataRegistry.getValue(ResourceLocation.parse(registryName));
        } catch (final ResourceLocationException ignored) {
            return null;
        }
    }

    public ItemStack withData(final ItemStack stack, final ResourceLocation key) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return ItemStack.EMPTY;
        }

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putString(StorageItemUtils.IMAGE_TAG_NAME, key.toString()));

        return stack;
    }

    public ItemStack withData(final ResourceLocation key) {
        return withData(new ItemStack(this), key);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void addCreativeTabItems(final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output) {
        output.accept(new ItemStack(this));

        BlockDeviceDataRegistry.firmwareValues().forEach(data -> {
            final ResourceLocation key = BlockDeviceDataRegistry.getKey(data);
            if (key == null) {
                return;
            }

            final ItemStack stack = withData(key);
            final DyeColor color = data.getColor();
            if (color != null) {
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color.getTextureDiffuseColor(), true));
            }
            output.accept(stack);
        });
    }

    @Override
    public int getColor(final ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, BLANK_COLOR);
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
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", Items.FLASH_MEMORY.getId());
        }
        return descriptionId;
    }
}
