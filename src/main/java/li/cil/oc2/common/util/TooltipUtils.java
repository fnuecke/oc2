package li.cil.oc2.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tags.ITag;
import net.minecraft.util.text.*;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.ArrayList;
import java.util.List;

import static li.cil.oc2.common.Constants.*;

public final class TooltipUtils {
    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IntList> ITEM_STACKS_SIZES = ThreadLocal.withInitial(IntArrayList::new);

    ///////////////////////////////////////////////////////////////////

    public static void tryAddDescription(final ItemStack stack, final List<ITextComponent> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        final String translationKey = stack.getTranslationKey() + Constants.TOOLTIP_DESCRIPTION_SUFFIX;
        final LanguageMap languagemap = LanguageMap.getInstance();
        if (languagemap.func_230506_b_(translationKey)) {
            final TranslationTextComponent description = new TranslationTextComponent(translationKey);
            tooltip.add(new StringTextComponent("").modifyStyle(s -> s.setColor(Color.fromTextFormatting(TextFormatting.GRAY))).append(description));
        }

        // Tooltips get queried very early in Minecraft initialization, meaning tags may not
        // have been initialized. Trying to directly use our tag would lead to an exception
        // in that case, so we do the detour through the collection instead.
        final ITag<Item> tag = net.minecraft.tags.ItemTags.getCollection().get(ItemTags.DEVICE_NEEDS_REBOOT.getName());
        if (tag != null && tag.contains(stack.getItem())) {
            tooltip.add(new StringTextComponent("").modifyStyle(s -> s.setColor(Color.fromTextFormatting(TextFormatting.YELLOW)))
                    .append(new TranslationTextComponent(Constants.TOOLTIP_DEVICE_NEEDS_REBOOT)));
        }
    }

    public static void addTileEntityInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addInventoryInformation(NBTUtils.getChildTag(stack.getTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME), tooltip);
    }

    public static void addEntityInventoryInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addInventoryInformation(NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME, ITEMS_TAG_NAME), tooltip);
    }

    public static void addInventoryInformation(final CompoundNBT itemsTag, final List<ITextComponent> tooltip) {
        addInventoryInformation(itemsTag, tooltip, getDeviceTypeNames());
    }

    public static void addInventoryInformation(final CompoundNBT itemsTag, final List<ITextComponent> tooltip, final String... subInventoryNames) {
        final List<ItemStack> itemStacks = ITEM_STACKS.get();
        itemStacks.clear();
        final IntList itemStackSizes = ITEM_STACKS_SIZES.get();
        itemStackSizes.clear();

        collectItemStacks(itemsTag, itemStacks, itemStackSizes);

        for (final String subInventoryName : subInventoryNames) {
            if (itemsTag.contains(subInventoryName, NBTTagIds.TAG_COMPOUND)) {
                collectItemStacks(itemsTag.getCompound(subInventoryName), itemStacks, itemStackSizes);
            }
        }

        for (int i = 0; i < itemStacks.size(); i++) {
            final ItemStack itemStack = itemStacks.get(i);
            tooltip.add(new StringTextComponent("- ")
                    .append(itemStack.getDisplayName())
                    .modifyStyle(style -> style.setColor(Color.fromTextFormatting(TextFormatting.GRAY)))
                    .append(new StringTextComponent(" x")
                            .appendString(String.valueOf(itemStackSizes.getInt(i)))
                            .modifyStyle(style -> style.setColor(Color.fromTextFormatting(TextFormatting.DARK_GRAY))))
            );
        }
    }

    public static void addTileEntityEnergyInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addEnergyInformation(NBTUtils.getChildTag(stack.getTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ENERGY_TAG_NAME), tooltip);
    }

    public static void addEntityEnergyInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        addEnergyInformation(NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME, ENERGY_TAG_NAME), tooltip);
    }

    public static void addEnergyInformation(final CompoundNBT energyTag, final List<ITextComponent> tooltip) {
        final int stored = energyTag.getInt(FixedEnergyStorage.STORED_TAG_NAME);
        if (stored == 0) {
            return;
        }

        final int capacity = energyTag.getInt(FixedEnergyStorage.CAPACITY_TAG_NAME);
        if (capacity > 0) {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_ENERGY, stored + "/" + capacity));
        } else {
            tooltip.add(new TranslationTextComponent(Constants.TOOLTIP_ENERGY, stored));
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static String[] getDeviceTypeNames() {
        final ForgeRegistry<DeviceType> registry = RegistryManager.ACTIVE.getRegistry(DeviceType.REGISTRY);
        if (registry != null) {
            return registry.getValues().stream().map(deviceType ->
                    deviceType.getRegistryName().toString()).toArray(String[]::new);
        } else {
            return new String[0];
        }
    }

    private static void collectItemStacks(final CompoundNBT tag, final List<ItemStack> stacks, final IntList stackSizes) {
        final ListNBT itemsTag = tag.getList("Items", NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            final CompoundNBT itemTag = itemsTag.getCompound(i);
            final ItemStack itemStack = ItemStack.read(itemTag);

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
