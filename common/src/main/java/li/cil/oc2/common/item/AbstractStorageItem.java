/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.TextFormatUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
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

    // --------------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
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
        if (!tag.getInt(CAPACITY_TAG_NAME).isPresent()) {
            return defaultCapacity.getAsInt();
        }

        final int capacity = tag.getIntOr(CAPACITY_TAG_NAME, 0);
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
