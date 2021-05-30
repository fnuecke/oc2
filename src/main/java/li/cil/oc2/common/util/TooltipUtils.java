package li.cil.oc2.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static li.cil.oc2.common.Constants.*;

public final class TooltipUtils {
    private static final MutableComponent DEVICE_NEEDS_REBOOT =
            new TranslatableComponent(Constants.TOOLTIP_DEVICE_NEEDS_REBOOT)
                    .withStyle(s -> s.withColor(ChatFormatting.YELLOW));

    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IntList> ITEM_STACKS_SIZES = ThreadLocal.withInitial(IntArrayList::new);

    ///////////////////////////////////////////////////////////////////

    public static void tryAddDescription(final ItemStack stack, final List<Component> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        final String translationKey = stack.getDescriptionId() + Constants.TOOLTIP_DESCRIPTION_SUFFIX;
        final Language languagemap = Language.getInstance();
        if (languagemap.has(translationKey)) {
            final TranslatableComponent description = new TranslatableComponent(translationKey);
            tooltip.add(withColor(description, ChatFormatting.GRAY));
        }

        // Tooltips get queried very early in Minecraft initialization, meaning tags may not
        // have been initialized. Trying to directly use our tag would lead to an exception
        // in that case, so we do the detour through the collection instead.
        final Tag<Item> tag = net.minecraft.tags.ItemTags.getAllTags().getTag(ItemTags.DEVICE_NEEDS_REBOOT.getName());
        if (tag != null && tag.contains(stack.getItem())) {
            tooltip.add(DEVICE_NEEDS_REBOOT);
        }

        final ItemDeviceQuery query = Devices.makeQuery(stack);
        final int energyConsumption = Devices.getEnergyConsumption(query);
        if (energyConsumption > 0) {
            final MutableComponent energy = withColor(String.valueOf(energyConsumption), ChatFormatting.GREEN);
            tooltip.add(withColor(new TranslatableComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, energy), ChatFormatting.GRAY));
        }
    }

    public static void addBlockEntityInventoryInformation(final ItemStack stack, final List<Component> tooltip) {
        addInventoryInformation(NBTUtils.getChildTag(stack.getTag(), BLOCK_ENTITY_TAG_NAME_IN_ITEM, ITEMS_TAG_NAME), tooltip);
    }

    public static void addEntityInventoryInformation(final ItemStack stack, final List<Component> tooltip) {
        addInventoryInformation(NBTUtils.getChildTag(stack.getTag(), MOD_TAG_NAME, ITEMS_TAG_NAME), tooltip);
    }

    public static void addInventoryInformation(final CompoundTag itemsTag, final List<Component> tooltip) {
        addInventoryInformation(itemsTag, tooltip, getDeviceTypeNames());
    }

    public static void addInventoryInformation(final CompoundTag itemsTag, final List<Component> tooltip, final String... subInventoryNames) {
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
            tooltip.add(new TextComponent("- ")
                    .append(itemStack.getDisplayName())
                    .withStyle(style -> style.withColor(ChatFormatting.GRAY))
                    .append(new TextComponent(" x")
                            .append(String.valueOf(itemStackSizes.getInt(i)))
                            .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)))
            );
        }
    }

    public static void addEntityEnergyInformation(final ItemStack stack, final List<Component> tooltip) {
        stack.getCapability(Capabilities.ENERGY_STORAGE).ifPresent(energy -> {
            if (energy.getEnergyStored() == 0) {
                return;
            }

            final MutableComponent value = withColor(energy.getEnergyStored() + "/" + energy.getMaxEnergyStored(), ChatFormatting.GREEN);
            tooltip.add(withColor(new TranslatableComponent(Constants.TOOLTIP_ENERGY, value), ChatFormatting.GRAY));
        });
    }

    public static void addEnergyConsumption(final double value, final List<Component> tooltip) {
        if (value > 0) {
            tooltip.add(withColor(new TranslatableComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, withColor(new DecimalFormat("#.##").format(value), ChatFormatting.GREEN)), ChatFormatting.GRAY));
        }
    }

    public static MutableComponent withColor(final String value, final ChatFormatting formatting) {
        return withColor(new TextComponent(value), formatting);
    }

    public static MutableComponent withColor(final MutableComponent text, final ChatFormatting formatting) {
        return text.withStyle(s -> s.withColor(formatting));
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

    private static void collectItemStacks(final CompoundTag tag, final List<ItemStack> stacks, final IntList stackSizes) {
        final ListTag itemsTag = tag.getList("Items", NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            final CompoundTag itemTag = itemsTag.getCompound(i);
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
