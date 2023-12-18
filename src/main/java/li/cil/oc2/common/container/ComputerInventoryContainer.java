/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.vm.VMItemStackHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.NetworkHooks;

public final class ComputerInventoryContainer extends AbstractComputerContainer {
    public static void createServer(final ComputerBlockEntity computer, final IEnergyStorage energy, final CommonDeviceBusController busController, final ServerPlayer player) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable(computer.getBlockState().getBlock().getDescriptionId());
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player player) {
                return new ComputerInventoryContainer(id, computer, player, createEnergyInfo(energy, busController));
            }
        }, computer.getBlockPos());
    }

    public static ComputerInventoryContainer createClient(final int id, final Inventory playerInventory, final FriendlyByteBuf data) {
        final BlockPos pos = data.readBlockPos();
        final BlockEntity blockEntity = playerInventory.player.level.getBlockEntity(pos);
        if (blockEntity instanceof final ComputerBlockEntity computer) {
            return new ComputerInventoryContainer(id, computer, playerInventory.player, createClientEnergyInfo());
        }

        throw new IllegalArgumentException();
    }

    ///////////////////////////////////////////////////////////////////

    private ComputerInventoryContainer(final int id, final ComputerBlockEntity computer, final Player player, final IntPrecisionContainerData energyInfo) {
        super(Containers.COMPUTER.get(), id, player, computer, energyInfo);

        final VMItemStackHandlers handlers = computer.getItemStackHandlers();

        handlers.getItemHandler(DeviceTypes.FLASH_MEMORY).ifPresent(itemHandler -> {
            if (itemHandler.getSlots() > 0) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.FLASH_MEMORY, 0, 64, 78));
            }
        });

        handlers.getItemHandler(DeviceTypes.MEMORY).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.MEMORY, slot, 64 + slot * SLOT_SIZE, 24));
            }
        });

        handlers.getItemHandler(DeviceTypes.HARD_DRIVE).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.HARD_DRIVE, slot, 100 + (slot % 2) * SLOT_SIZE, 60 + (slot / 2) * SLOT_SIZE));
            }
        });

        handlers.getItemHandler(DeviceTypes.CARD).ifPresent(itemHandler -> {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                addSlot(new DeviceTypeSlotItemHandler(itemHandler, DeviceTypes.CARD, slot, 38, 24 + slot * SLOT_SIZE));
            }
        });

        createPlayerInventoryAndHotbarSlots(player.getInventory(), 8, 115);
    }
}
