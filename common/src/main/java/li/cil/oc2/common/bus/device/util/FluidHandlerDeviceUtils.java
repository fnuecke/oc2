/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ContainerFluidHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.WorldFluidHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

public final class FluidHandlerDeviceUtils {
    public static int requireValidTank(final FluidHandler handler, final int tank) {
        if (tank < 0 || tank >= handler.getTanks()) {
            throw new IllegalArgumentException("tank out of range: " + tank
                + " (expected 0 to " + (handler.getTanks() - 1) + ")");
        }
        return tank;
    }

    public static List<FluidHandler> requireFluidHandlers(final ServerLevel level, final BlockPos pos, final Direction side,
                                                          @Nullable final Entity except, final Object sideName) {
        final List<FluidHandler> handlers = Capabilities.getAll(level, pos, Capabilities.FLUID_HANDLER, side, except);
        if (handlers.isEmpty()) {
            throw new IllegalArgumentException("no fluid container on side: " + sideName);
        }
        return handlers;
    }

    public static List<FluidHandler> requireFluidHandlersOrWorld(final ServerLevel level, final BlockPos pos, final Direction side,
                                                                 @Nullable final Entity except, final Player player, final Object sideName) {
        final List<FluidHandler> handlers = Capabilities.getAll(level, pos, Capabilities.FLUID_HANDLER, side, except);
        if (!handlers.isEmpty()) {
            return handlers;
        }

        LevelUtils.requireWorldAccess(level, pos, player);
        final FluidHandler world = WorldFluidHandler.of(level, pos);
        if (world == null) {
            throw new IllegalArgumentException("no fluid container on side: " + sideName);
        }
        return List.of(world);
    }

    public static FluidHandler requireContainerItem(final ItemHandler inventory, final int slot) {
        final FluidHandler container = ContainerFluidHandler.of(inventory, slot);
        if (container == null) {
            throw new IllegalArgumentException("no container item in selected slot");
        }
        return container;
    }

    // --------------------------------------------------------------------- //

    private FluidHandlerDeviceUtils() {
    }
}
