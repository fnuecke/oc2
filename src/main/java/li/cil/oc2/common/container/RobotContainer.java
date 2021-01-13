package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.vm.VirtualMachineItemStackHandlers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;

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

    public RobotContainer(final int id, final RobotEntity robot, final PlayerInventory inventory) {
        super(Containers.ROBOT_CONTAINER.get(), id);
        this.robot = robot;

        final VirtualMachineItemStackHandlers handlers = robot.getItemStackHandlers();

        handlers.getItemHandler(DeviceTypes.FLASH_MEMORY).ifPresent(itemHandler -> {
            if (itemHandler.getSlots() > 0) {
                this.addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.FLASH_MEMORY, 0, 64, 78));
            }
        });

        handlers.getItemHandler(DeviceTypes.MEMORY).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                this.addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.MEMORY, slot, 64 + slot * SLOT_SIZE, 24));
            }
        });

        handlers.getItemHandler(DeviceTypes.HARD_DRIVE).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                this.addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.HARD_DRIVE, slot, 100 + (slot % 2) * SLOT_SIZE, 60 + (slot / 2) * SLOT_SIZE));
            }
        });

        handlers.getItemHandler(DeviceTypes.CARD).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                this.addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.CARD, slot, 38, 24 + slot * SLOT_SIZE));
            }
        });

        createPlayerInventoryAndHotbarSlots(inventory, 8, 115);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean canInteractWith(final PlayerEntity player) {
        return robot.isEntityInRange(player, 8);
    }
}
