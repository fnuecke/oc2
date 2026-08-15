/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.TextFormatUtils;
import li.cil.oc2.common.util.TooltipUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public abstract class AbstractStorageItem extends ModItem {
    private static final String CAPACITY_TAG_NAME = "capacity";

    // ------------------------------------------------------------- //

    private final int defaultCapacity;

    // ------------------------------------------------------------- //

    protected AbstractStorageItem(final Properties properties, final int defaultCapacity) {
        super(properties);
        this.defaultCapacity = defaultCapacity;
    }

    protected AbstractStorageItem(final int capacity) {
        this(createProperties(), capacity);
    }

    // ------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TooltipUtils.addDataCorrupted(stack, tooltip);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final int capacity = getCapacity(stack);
        return Component.literal("")
                .append(super.getName(stack))
                .append(" (")
                .append(TextFormatUtils.formatSize(capacity))
                .append(")");
    }

    // ------------------------------------------------------------- //

    public int getCapacity(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (!tag.contains(CAPACITY_TAG_NAME, NBTTagIds.TAG_INT)) {
            return defaultCapacity;
        }

        final int capacity = tag.getInt(CAPACITY_TAG_NAME);
        if (Config.maxBlobCapacity <= 0) {
            return Math.max(capacity, 0);
        }

        return Mth.clamp(capacity, 0, Config.maxBlobCapacity);
    }

    public ItemStack withCapacity(final ItemStack stack, final int capacity) {
        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putInt(CAPACITY_TAG_NAME, capacity));
        return stack;
    }

    public ItemStack withCapacity(final int capacity) {
        return withCapacity(new ItemStack(this), capacity);
    }
}
