/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.TextFormatUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.IntSupplier;

public abstract class AbstractStorageItem extends ModItem {
    private static final String CAPACITY_TAG_NAME = "capacity";

    // --------------------------------------------------------------------- //

    private final IntSupplier defaultCapacity;

    // --------------------------------------------------------------------- //

    protected AbstractStorageItem(final Properties properties, final IntSupplier defaultCapacity) {
        super(properties);
        this.defaultCapacity = defaultCapacity;
    }

    protected AbstractStorageItem(final IntSupplier defaultCapacity) {
        this(createProperties(), defaultCapacity);
    }

    // --------------------------------------------------------------------- //

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

    // --------------------------------------------------------------------- //

    public int getCapacity(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (tag.contains(CAPACITY_TAG_NAME, NBTTagIds.TAG_INT)) {
            return clampCapacity(tag.getInt(CAPACITY_TAG_NAME));
        }

        final int imageCapacity = getImageCapacity(stack);
        return clampCapacity(imageCapacity > 0 ? imageCapacity : defaultCapacity.getAsInt());
    }

    public ItemStack withCapacity(final ItemStack stack, final int capacity) {
        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putInt(CAPACITY_TAG_NAME, capacity));
        return stack;
    }

    public ItemStack withCapacity(final int capacity) {
        return withCapacity(new ItemStack(this), capacity);
    }

    public boolean initializeCapacity(final ItemStack stack) {
        return updateCapacity(stack, clampCapacity(Math.max(getCapacity(stack), getImageCapacity(stack))));
    }

    public boolean updateCapacity(final ItemStack stack, final int capacity) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (tag.contains(CAPACITY_TAG_NAME, NBTTagIds.TAG_INT) && tag.getInt(CAPACITY_TAG_NAME) == capacity) {
            return false;
        }

        withCapacity(stack, capacity);
        return true;
    }

    // --------------------------------------------------------------------- //

    protected int getImageCapacity(final ItemStack stack) {
        return 0;
    }

    // --------------------------------------------------------------------- //

    private static int clampCapacity(final int capacity) {
        if (Config.maxBlobCapacity <= 0) {
            return Math.max(capacity, 0);
        }

        return Mth.clamp(capacity, 0, Config.maxBlobCapacity);
    }
}
