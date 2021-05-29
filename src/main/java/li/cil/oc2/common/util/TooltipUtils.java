package li.cil.oc2.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tags.ITag;
import net.minecraft.util.text.*;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static li.cil.oc2.common.Constants.*;

public final class TooltipUtils {
    private static final IFormattableTextComponent DEVICE_NEEDS_REBOOT =
            new TranslationTextComponent(Constants.TOOLTIP_DEVICE_NEEDS_REBOOT)
                    .withStyle(s -> s.withColor(Color.fromLegacyFormat(TextFormatting.YELLOW)));

    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IntList> ITEM_STACKS_SIZES = ThreadLocal.withInitial(IntArrayList::new);

    ///////////////////////////////////////////////////////////////////

    public static void tryAddDescription(final ItemStack stack, final List<ITextComponent> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        final String translationKey = stack.getDescriptionId() + Constants.TOOLTIP_DESCRIPTION_SUFFIX;
        final LanguageMap languagemap = LanguageMap.getInstance();
        if (languagemap.has(translationKey)) {
            final TranslationTextComponent description = new TranslationTextComponent(translationKey);
            tooltip.add(withColor(description, TextFormatting.GRAY));
        }

        // Tooltips get queried very early in Minecraft initialization, meaning tags may not
        // have been initialized. Trying to directly use our tag would lead to an exception
        // in that case, so we do the detour through the collection instead.
        final ITag<Item> tag = net.minecraft.tags.ItemTags.getAllTags().getTag(ItemTags.DEVICE_NEEDS_REBOOT.getName());
        if (tag != null && tag.contains(stack.getItem())) {
            tooltip.add(DEVICE_NEEDS_REBOOT);
        }

        final ItemDeviceQuery query = Devices.makeQuery(stack);
        final int energyConsumption = Devices.getEnergyConsumption(query);
        if (energyConsumption > 0) {
            final IFormattableTextComponent energy = withColor(String.valueOf(energyConsumption), TextFormatting.GREEN);
            tooltip.add(withColor(new TranslationTextComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, energy), TextFormatting.GRAY));
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
                    .withStyle(style -> style.withColor(Color.fromLegacyFormat(TextFormatting.GRAY)))
                    .append(new StringTextComponent(" x")
                            .append(String.valueOf(itemStackSizes.getInt(i)))
                            .withStyle(style -> style.withColor(Color.fromLegacyFormat(TextFormatting.DARK_GRAY))))
            );
        }
    }

    public static void addEntityEnergyInformation(final ItemStack stack, final List<ITextComponent> tooltip) {
        stack.getCapability(Capabilities.ENERGY_STORAGE).ifPresent(energy -> {
            if (energy.getEnergyStored() == 0) {
                return;
            }

            final IFormattableTextComponent value = withColor(energy.getEnergyStored() + "/" + energy.getMaxEnergyStored(), TextFormatting.GREEN);
            tooltip.add(withColor(new TranslationTextComponent(Constants.TOOLTIP_ENERGY, value), TextFormatting.GRAY));
        });
    }

    public static void addEnergyConsumption(final double value, final List<ITextComponent> tooltip) {
        if (value > 0) {
            tooltip.add(withColor(new TranslationTextComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, withColor(new DecimalFormat("#.##").format(value), TextFormatting.GREEN)), TextFormatting.GRAY));
        }
    }

    public static IFormattableTextComponent withColor(final String value, final TextFormatting formatting) {
        return withColor(new StringTextComponent(value), formatting);
    }

    public static IFormattableTextComponent withColor(final IFormattableTextComponent text, final TextFormatting formatting) {
        return text.withStyle(s -> s.withColor(Color.fromLegacyFormat(formatting)));
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
            final ItemStack itemStack = ItemStack.of(itemTag);

            boolean didMerge = false;
            for (int j = 0; j < stacks.size(); j++) {
                final ItemStack existingStack = stacks.get(j);
                if (ItemStack.matches(existingStack, itemStack) &&
                    ItemStack.matches(existingStack, itemStack)) {
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
