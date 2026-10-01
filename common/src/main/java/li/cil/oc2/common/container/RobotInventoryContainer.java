/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.energy.FixedEnergyHandler;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.fluid.FluidTank;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.vm.VMItemStackHandlers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class RobotInventoryContainer extends AbstractRobotContainer {
    private static final int FLUID_ID_INDEX = 0;
    private static final int FLUID_AMOUNT_INDEX = 1;
    private static final int FLUID_INFO_SIZE = 2;

    // --------------------------------------------------------------------- //

    private final IntPrecisionContainerData fluidInfo;

    // --------------------------------------------------------------------- //

    public static void createServer(final Robot robot, final FixedEnergyHandler energy, final CommonDeviceBusController busController, final ServerPlayer player) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public Component getDisplayName() {
                return robot.getName();
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                return new RobotInventoryContainer(id, robot, player, createEnergyInfo(energy, busController), createFluidInfo(robot.getTank()));
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
            return new RobotInventoryContainer(id, robot, inventory.player, createClientEnergyInfo(), new IntPrecisionContainerData.Client(FLUID_INFO_SIZE));
        }

        throw new IllegalArgumentException();
    }

    // --------------------------------------------------------------------- //

    private RobotInventoryContainer(final int id, final Robot robot, final Player player, final IntPrecisionContainerData energyInfo, final IntPrecisionContainerData fluidInfo) {
        super(Containers.ROBOT.get(), id, player, robot, energyInfo);
        this.fluidInfo = fluidInfo;

        checkContainerDataCount(fluidInfo, FLUID_INFO_SIZE);
        addDataSlots(fluidInfo);

        final VMItemStackHandlers handlers = robot.getItemStackHandlers();

        handlers.getItemHandler(DeviceTypes.CPU.get()).ifPresent(itemHandler -> {
            if (itemHandler.getSlots() > 0) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.CPU.get(), 0, 34, 52));
            }
        });

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
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.HARD_DRIVE.get(), slot, 70, 60 + slot * SLOT_SIZE));
            }
        });

        handlers.getItemHandler(DeviceTypes.FLOPPY.get()).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.FLOPPY.get(), slot, 70 + SLOT_SIZE, 60 + slot * SLOT_SIZE));
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
            final int y = 20 + slot / 3 * SLOT_SIZE;
            addSlot(new RobotSlot(inventory, slot, x, y));
        }

        createPlayerInventoryAndHotbarSlots(player.getInventory(), 8, 115);
    }

    // --------------------------------------------------------------------- //

    public FluidStack getFluid() {
        return new FluidStack(BuiltInRegistries.FLUID.byId(fluidInfo.getInt(FLUID_ID_INDEX)), fluidInfo.getInt(FLUID_AMOUNT_INDEX));
    }

    public int getFluidCapacity() {
        return getRobot().getTank().getTankCapacity(0);
    }

    // --------------------------------------------------------------------- //

    private static IntPrecisionContainerData createFluidInfo(final FluidTank tank) {
        return new IntPrecisionContainerData.Server() {
            @Override
            public int getInt(final int index) {
                return switch (index) {
                    case FLUID_ID_INDEX -> BuiltInRegistries.FLUID.getId(tank.getFluid().fluid());
                    case FLUID_AMOUNT_INDEX -> tank.getFluid().amount();
                    default -> 0;
                };
            }

            @Override
            public int getIntCount() {
                return FLUID_INFO_SIZE;
            }
        };
    }
}
