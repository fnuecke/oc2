/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ContainerItemHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.inventory.WorldItemHandler;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

public final class ItemHandlerDeviceUtils {
    public static int requireValidSlot(final ItemHandler handler, final int slot) {
        if (slot < 0 || slot >= handler.getSlots()) {
            throw new IllegalArgumentException("slot out of range: " + slot
                + " (expected 0 to " + (handler.getSlots() - 1) + ")");
        }
        return slot;
    }

    public static List<ItemHandler> requireItemHandlersWithSlot(final List<ItemHandler> handlers, final int slot) {
        final List<ItemHandler> result = handlers.stream()
            .filter(handler -> slot >= 0 && slot < handler.getSlots())
            .toList();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("slot out of range: " + slot);
        }
        return result;
    }

    public static List<ItemHandler> requireItemHandlers(final ServerLevel level, final BlockPos pos, final Direction side,
                                                        @Nullable final Entity except, final Object sideName) {
        final List<ItemHandler> handlers = Capabilities.getAll(level, pos, Capabilities.ITEM_HANDLER, side, except);
        if (handlers.isEmpty()) {
            throw new IllegalArgumentException("no inventory on side: " + sideName);
        }
        return handlers;
    }

    public static List<ItemHandler> requireItemHandlersOrWorld(final ServerLevel level, final BlockPos pos, final Direction side,
                                                               @Nullable final Entity except, final Player player, final Object sideName) {
        final List<ItemHandler> handlers = Capabilities.getAll(level, pos, Capabilities.ITEM_HANDLER, side, except);
        if (!handlers.isEmpty()) {
            return handlers;
        }

        LevelUtils.requireWorldAccess(level, pos, player);
        final ItemHandler world = WorldItemHandler.of(level, pos);
        if (world == null) {
            throw new IllegalArgumentException("no inventory on side: " + sideName);
        }
        return List.of(world);
    }

    public static ItemHandler requireContainerItem(final ItemHandler inventory, final int slot) {
        final ItemHandler container = ContainerItemHandler.of(inventory, slot);
        if (container == null) {
            throw new IllegalArgumentException("no container item in selected slot");
        }
        return container;
    }

    // --------------------------------------------------------------------- //

    private ItemHandlerDeviceUtils() {
    }
}
