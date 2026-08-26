/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.vm.VMItemStackHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class RobotInventoryContainer extends AbstractRobotContainer {
    public static void createServer(final Robot robot, final FixedEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public Component getDisplayName() {
                return robot.getName();
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                return new RobotInventoryContainer(id, robot, player, createEnergyInfo(energy, busController));
            }

            @Override
            public void saveExtraData(final FriendlyByteBuf buffer) {
                buffer.writeVarInt(robot.getId());
            }
        });
    }

    public static RobotInventoryContainer createClient(final int id, final Inventory inventory, final FriendlyByteBuf data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.level().getEntity(entityId);
        if (entity instanceof final Robot robot) {
            return new RobotInventoryContainer(id, robot, inventory.player, createClientEnergyInfo());
        }

        throw new IllegalArgumentException();
    }

    // --------------------------------------------------------------------- //

    private RobotInventoryContainer(final int id, final Robot robot, final Player player, final IntPrecisionContainerData energyInfo) {
        super(Containers.ROBOT.get(), id, player, robot, energyInfo);

        final VMItemStackHandlers handlers = robot.getItemStackHandlers();

        handlers.getItemHandler(DeviceTypes.FLASH_MEMORY.get()).ifPresent(itemHandler -> {
            if (itemHandler.getSlots() > 0) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.FLASH_MEMORY.get(), 0, 34, 78));
            }
        });

        handlers.getItemHandler(DeviceTypes.MEMORY.get()).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.MEMORY.get(), slot, 34 + slot * SLOT_SIZE, 24));
            }
        });

        handlers.getItemHandler(DeviceTypes.HARD_DRIVE.get()).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.HARD_DRIVE.get(), slot, 70 + slot % 2 * SLOT_SIZE, 60 + slot / 2 * SLOT_SIZE));
            }
        });

        handlers.getItemHandler(DeviceTypes.ROBOT_MODULE.get()).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.ROBOT_MODULE.get(), slot, 8, 24 + slot * SLOT_SIZE));
            }
        });

        final ItemHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final int x = 116 + slot % 3 * SLOT_SIZE;
            final int y = 24 + slot / 3 * SLOT_SIZE;
            addSlot(new RobotSlot(inventory, slot, x, y));
        }

        createPlayerInventoryAndHotbarSlots(player.getInventory(), 8, 115);
    }
}
