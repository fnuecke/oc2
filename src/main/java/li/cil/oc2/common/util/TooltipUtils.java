package li.cil.oc2.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import li.cil.oc2.api.bus.device.DeviceType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class TooltipUtils {
    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IntList> ITEM_STACKS_SIZES = ThreadLocal.withInitial(IntArrayList::new);

    public static void addTileEntityInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addInventoryInformation(ItemStackUtils.getTileEntityInventoryTag(stack), tooltip);
    }

    public static void addTileEntityInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip, final String... subInventoryNames) {
        addInventoryInformation(ItemStackUtils.getTileEntityInventoryTag(stack), tooltip, subInventoryNames);
    }

    public static void addEntityInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addInventoryInformation(ItemStackUtils.getEntityInventoryTag(stack), tooltip);
    }

    public static void addEntityInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip, final String... subInventoryNames) {
        addInventoryInformation(ItemStackUtils.getEntityInventoryTag(stack), tooltip, subInventoryNames);
    }

    public static void addInventoryInformation(@Nullable final CompoundNBT inventoryTag, final List<ITextComponent> tooltip) {
        addInventoryInformation(inventoryTag, tooltip, getDeviceTypeNames());
    }

    public static void addInventoryInformation(@Nullable final CompoundNBT inventoryTag, final List<ITextComponent> tooltip, final String... subInventoryNames) {
        if (inventoryTag == null) {
            return;
        }

        final List<ItemStack> itemStacks = ITEM_STACKS.get();
        itemStacks.clear();
        final IntList itemStackSizes = ITEM_STACKS_SIZES.get();
        itemStackSizes.clear();

        collectItemStacks(inventoryTag, itemStacks, itemStackSizes);

        for (final String subInventoryName : subInventoryNames) {
            if (inventoryTag.contains(subInventoryName, NBTTagIds.TAG_COMPOUND)) {
                collectItemStacks(inventoryTag.getCompound(subInventoryName), itemStacks, itemStackSizes);
            }
        }

        for (int i = 0; i < itemStacks.size(); i++) {
            final ItemStack itemStack = itemStacks.get(i);
            tooltip.add(new StringTextComponent("")
                    .append(itemStack.getDisplayName())
                    .modifyStyle(style -> style.setColor(Color.fromTextFormatting(TextFormatting.GRAY)))
                    .append(new StringTextComponent(" x")
                            .appendString(String.valueOf(itemStackSizes.getInt(i)))
                            .modifyStyle(style -> style.setColor(Color.fromTextFormatting(TextFormatting.DARK_GRAY))))
            );
        }
    }

    private static String[] getDeviceTypeNames() {
        final ForgeRegistry<DeviceType> registry = RegistryManager.ACTIVE.getRegistry(DeviceType.REGISTRY);
        if (registry != null) {
            return registry.getValues().stream().map(deviceType ->
                    deviceType.getRegistryName().toString()).toArray(String[]::new);
        } else {
            return new String[0];
        }
    }

    private static void collectItemStacks(final CompoundNBT nbt, final List<ItemStack> stacks, final IntList stackSizes) {
        final ListNBT itemsNbt = nbt.getList("Items", NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < itemsNbt.size(); i++) {
            final CompoundNBT itemNbt = itemsNbt.getCompound(i);
            final ItemStack itemStack = ItemStack.read(itemNbt);

            boolean didMerge = false;
            for (int j = 0; j < stacks.size(); j++) {
                final ItemStack existingStack = stacks.get(j);
                if (ItemStack.areItemsEqual(existingStack, itemStack) &&
                    ItemStack.areItemStackTagsEqual(existingStack, itemStack)) {
                    final int existingCount = stackSizes.getInt(j);
                    stackSizes.set(j, existingCount + itemStack.getCount());
                    didMerge = true;
                    break;
                }
            }

            if (!didMerge) {
                stacks.add(itemStack);
                stackSizes.add(itemStack.getCount());
            }
        }
    }
}
