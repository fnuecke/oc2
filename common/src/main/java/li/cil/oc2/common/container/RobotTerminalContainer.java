/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.Robot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class RobotTerminalContainer extends AbstractRobotContainer {
    public static void createServer(final Robot robot, final FixedEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public Component getDisplayName() {
                return robot.getName();
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                return new RobotTerminalContainer(id, player, robot, createEnergyInfo(energy, busController));
            }

            @Override
            public void saveExtraData(final FriendlyByteBuf buffer) {
                buffer.writeVarInt(robot.getId());
            }
        });
    }

    public static RobotTerminalContainer createClient(final int id, final Inventory inventory, final FriendlyByteBuf data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.level().getEntity(entityId);
        if (entity instanceof final Robot robot) {
            return new RobotTerminalContainer(id, inventory.player, robot, createClientEnergyInfo());
        }

        throw new IllegalArgumentException();
    }

    // ------------------------------------------------------------- //

    private RobotTerminalContainer(final int id, final Player player, final Robot robot, final IntPrecisionContainerData energyInfo) {
        super(Containers.ROBOT_TERMINAL.get(), id, player, robot, energyInfo);

        // It's kinda dumb we need to access technically-client-side stuff here, but that's the nature of containers
        // needing to specify display positions for some reason.
        final int terminalScreenWidth = Sprites.TERMINAL_SCREEN.width;
        final int terminalScreenHeight = Sprites.TERMINAL_SCREEN.height;

        final ItemHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final int x = (terminalScreenWidth - inventory.getSlots() * SLOT_SIZE) / 2 + 1 + slot * SLOT_SIZE;
            addSlot(new RobotSlot(inventory, slot, x, terminalScreenHeight + 4));
        }
    }
}
