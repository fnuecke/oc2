package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.vm.VMItemStackHandlers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class RobotInventoryContainer extends AbstractRobotContainer {
    public static void createServer(final RobotEntity robot, final FixedEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayerEntity player) {
        NetworkHooks.openGui(player, new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return robot.getName();
            }

            @Override
            public Container createMenu(final int id, final PlayerInventory inventory, final PlayerEntity player) {
                return new RobotInventoryContainer(id, robot, player, createEnergyInfo(energy, busController));
            }
        }, b -> b.writeVarInt(robot.getId()));
    }

    public static RobotInventoryContainer createClient(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.getCommandSenderWorld().getEntity(entityId);
        if (!(entity instanceof RobotEntity)) {
            throw new IllegalArgumentException();
        }
        return new RobotInventoryContainer(id, (RobotEntity) entity, inventory.player, createEnergyInfo());
    }

    ///////////////////////////////////////////////////////////////////

    private RobotInventoryContainer(final int id, final RobotEntity robot, final PlayerEntity player, final IIntArray energyInfo) {
        super(Containers.ROBOT.get(), id, robot, energyInfo);

        final VMItemStackHandlers handlers = robot.getItemStackHandlers();

        handlers.getItemHandler(DeviceTypes.FLASH_MEMORY).ifPresent(itemHandler -> {
            if (itemHandler.getSlots() > 0) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.FLASH_MEMORY, 0, 34, 78));
            }
        });

        handlers.getItemHandler(DeviceTypes.MEMORY).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.MEMORY, slot, 34 + slot * SLOT_SIZE, 24));
            }
        });

        handlers.getItemHandler(DeviceTypes.HARD_DRIVE).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.HARD_DRIVE, slot, 70 + (slot % 2) * SLOT_SIZE, 60 + (slot / 2) * SLOT_SIZE));
            }
        });

        handlers.getItemHandler(DeviceTypes.ROBOT_MODULE).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.ROBOT_MODULE, slot, 8, 24 + slot * SLOT_SIZE));
            }
        });

        final ItemStackHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final int x = 116 + (slot % 3) * SLOT_SIZE;
            final int y = 24 + (slot / 3) * SLOT_SIZE;
            addSlot(new SlotItemHandler(inventory, slot, x, y));
        }

        createPlayerInventoryAndHotbarSlots(player.inventory, 8, 115);
    }
}
