/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public final class WorldItemHandler implements ItemHandler {
    private final Level level;
    private final BlockPos pos;

    // --------------------------------------------------------------------- //

    private WorldItemHandler(final Level level, final BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Nullable
    public static ItemHandler of(final Level level, final BlockPos pos) {
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return null;
        }
        return new WorldItemHandler(level, pos);
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty() || simulate) {
            return ItemStack.EMPTY;
        }

        final ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy(), 0, 0, 0);
        entity.setDefaultPickUpDelay();
        return level.addFreshEntity(entity) ? ItemStack.EMPTY : stack;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        final List<ItemEntity> entities = getItemEntities();
        if (amount <= 0 || entities.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final ItemStack reference = entities.getFirst().getItem();
        int remaining = Math.min(amount, reference.getMaxStackSize());
        int extracted = 0;
        for (final ItemEntity entity : entities) {
            if (remaining <= 0) {
                break;
            }

            final ItemStack stack = entity.getItem();
            if (!ItemStack.isSameItemSameComponents(stack, reference)) {
                continue;
            }

            final int taken = Math.min(stack.getCount(), remaining);
            remaining -= taken;
            extracted += taken;

            if (!simulate) {
                // Set a copy, so the change is sent to clients.
                final ItemStack left = stack.copy();
                left.shrink(taken);
                entity.setItem(left);
            }
        }

        return reference.copyWithCount(extracted);
    }

    // --------------------------------------------------------------------- //

    private List<ItemEntity> getItemEntities() {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), entity -> entity.isAlive() && !entity.getItem().isEmpty());
    }
}
