package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.tileentity.ComputerBlockEntity;
import li.cil.oc2.common.vm.VMContainerHelpers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.LevelPosCallable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public final class ComputerInventoryContainer extends AbstractContainer {
    @Nullable
    public static ComputerInventoryContainer create(final int id, final PlayerInventory playerInventory, final PacketBuffer data) {
        final BlockPos pos = data.readBlockPos();
        final BlockEntity tileEntity = playerInventory.player.getCommandSenderWorld().getBlockEntity(pos);
        if (!(tileEntity instanceof ComputerBlockEntity)) {
            return null;
        }
        return new ComputerInventoryContainer(id, (ComputerBlockEntity) tileEntity, playerInventory);
    }

    ///////////////////////////////////////////////////////////////////

    private final ComputerBlockEntity computer;

    ///////////////////////////////////////////////////////////////////

    public ComputerInventoryContainer(final int id, final ComputerBlockEntity computer, final PlayerInventory playerInventory) {
        super(Containers.COMPUTER_CONTAINER.get(), id);
        this.computer = computer;

        final VMContainerHelpers handlers = computer.getContainerHelpers();

        handlers.getItemHandler(DeviceTypes.FLASH_MEMORY).ifPresent(itemHandler -> {
            if (itemHandler.getSlots() > 0) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.FLASH_MEMORY, 0, 64, 78));
            }
        });

        handlers.getItemHandler(DeviceTypes.MEMORY).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.MEMORY, slot, 64 + slot * SLOT_SIZE, 24));
            }
        });

        handlers.getItemHandler(DeviceTypes.HARD_DRIVE).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.HARD_DRIVE, slot, 100 + (slot % 2) * SLOT_SIZE, 60 + (slot / 2) * SLOT_SIZE));
            }
        });

        handlers.getItemHandler(DeviceTypes.CARD).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new TypedSlotItemHandler(itemHandler, DeviceTypes.CARD, slot, 38, 24 + slot * SLOT_SIZE));
            }
        });

        createPlayerInventoryAndHotbarSlots(playerInventory, 8, 115);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean stillValid(final PlayerEntity player) {
        return stillValid(LevelPosCallable.create(computer.getLevel(), computer.getBlockPos()), player, Blocks.COMPUTER.get());
    }
}
