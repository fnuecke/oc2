/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import static li.cil.oc2.common.Constants.ITEMS_TAG_NAME;
import static li.cil.oc2.common.bus.device.DeviceTypeRegistry.key;
import static li.cil.oc2.common.util.NBTUtils.makeInventoryTag;
import static li.cil.oc2.common.util.TranslationUtils.text;

public final class ComputerItem extends ModBlockItem implements CreativeTabItemProvider {
    public ComputerItem(final Block block) {
        super(block);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void addCreativeTabItems(final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output) {
        output.accept(withFlash(parameters.holders()));
        output.accept(preconfigured(parameters.holders()));
    }

    // --------------------------------------------------------------------- //

    private ItemStack withFlash(final HolderLookup.Provider provider) {
        final ItemStack computer = new ItemStack(this);

        ItemStackUtils.modifyBlockEntityDataTag(computer, BlockEntities.COMPUTER.get(), tag -> {
            final var itemsTag = NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME);
            itemsTag.put(key(DeviceTypes.CPU.get()), makeInventoryTag(provider,
                new ItemStack(Items.CPU_RISCV.get())
            ));
            itemsTag.put(key(DeviceTypes.FLASH_MEMORY.get()), makeInventoryTag(provider,
                Items.FLASH_MEMORY.get().withData(FirmwareRegistry.RISCV.getId())
            ));
        });

        return computer;
    }

    private ItemStack preconfigured(final HolderLookup.Provider provider) {
        final ItemStack computer = withFlash(provider);

        ItemStackUtils.modifyBlockEntityDataTag(computer, BlockEntities.COMPUTER.get(), tag -> {
            final var itemsTag = NBTUtils.getOrCreateChildTag(tag, ITEMS_TAG_NAME);
            itemsTag.put(key(DeviceTypes.MEMORY.get()), makeInventoryTag(provider,
                new ItemStack(Items.MEMORY_LARGE.get()),
                new ItemStack(Items.MEMORY_LARGE.get()),
                new ItemStack(Items.MEMORY_LARGE.get()),
                new ItemStack(Items.MEMORY_LARGE.get())
            ));
            itemsTag.put(key(DeviceTypes.HARD_DRIVE.get()), makeInventoryTag(provider,
                Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId())
            ));
            itemsTag.put(key(DeviceTypes.CARD.get()), makeInventoryTag(provider,
                new ItemStack(Items.NETWORK_INTERFACE_CARD.get())
            ));
        });

        computer.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, text("block.{mod}.computer.preconfigured"));

        return computer;
    }
}
