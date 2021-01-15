package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.vm.VirtualMachineItemStackHandlers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public final class RobotContainer extends AbstractContainer {
    @Nullable
    public static RobotContainer create(final int id, final PlayerInventory inventory, final PacketBuffer data) {
        final int entityId = data.readVarInt();
        final Entity entity = inventory.player.getEntityWorld().getEntityByID(entityId);
        if (!(entity instanceof RobotEntity)) {
            return null;
        }
        return new RobotContainer(id, (RobotEntity) entity, inventory);
    }

    ///////////////////////////////////////////////////////////////////

    private final RobotEntity robot;

    ///////////////////////////////////////////////////////////////////

    public RobotContainer(final int id, final RobotEntity robot, final PlayerInventory playerInventory) {
        super(Containers.ROBOT_CONTAINER.get(), id);
        this.robot = robot;

        final VirtualMachineItemStackHandlers handlers = robot.getItemStackHandlers();

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

        createPlayerInventoryAndHotbarSlots(playerInventory, 8, 115);
    }

    ///////////////////////////////////////////////////////////////////

    public RobotEntity getRobot() {
        return robot;
    }

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        return robot.isEntityInRange(player, 8);
    }
}
