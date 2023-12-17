/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public abstract class AbstractBlockDeviceItem extends ModItem {
    public static final String DATA_TAG_NAME = "data";

    ///////////////////////////////////////////////////////////////////

    private final ResourceLocation defaultData;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockDeviceItem(final Properties properties, final ResourceLocation defaultData) {
        super(properties.stacksTo(1));
        this.defaultData = defaultData;
    }

    protected AbstractBlockDeviceItem(final ResourceLocation defaultData) {
        this(createProperties(), defaultData);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(DATA_TAG_NAME);

        ResourceLocation location = defaultData;
        if (!StringUtil.isNullOrEmpty(registryName)) {
            try {
                location = new ResourceLocation(registryName);
            } catch (final ResourceLocationException ignored) {
            }
        }

        return BlockDeviceDataRegistry.getValue(location);
    }

    public ItemStack withData(final ItemStack stack, final BlockDeviceData data) {
        if (stack.isEmpty() || stack.getItem() != this) {
            return ItemStack.EMPTY;
        }

        final ResourceLocation key = BlockDeviceDataRegistry.getKey(data);
        if (key == null) {
            return ItemStack.EMPTY;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).putString(DATA_TAG_NAME, key.toString());

        return stack;
    }

    public ItemStack withData(final BlockDeviceData data) {
        return withData(new ItemStack(this), data);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final BlockDeviceData data = getData(stack);
        if (data != null) {
            return Component.literal("")
                .append(super.getName(stack))
                .append(" (")
                .append(data.getDisplayName())
                .append(")");
        } else {
            return super.getName(stack);
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected ResourceLocation getDefaultData() {
        return defaultData;
    }
}
