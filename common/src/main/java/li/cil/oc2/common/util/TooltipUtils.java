/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.EnergyConsumingBlock;
import li.cil.oc2.common.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.EnergyStorage;
import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static li.cil.oc2.common.Constants.*;
import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

public final class TooltipUtils {
    private static final MutableComponent DEVICE_NEEDS_REBOOT =
        Component.translatable(Constants.TOOLTIP_DEVICE_NEEDS_REBOOT)
            .withStyle(s -> s.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)));

    private static final ThreadLocal<List<ItemStack>> ITEM_STACKS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IntList> ITEM_STACKS_SIZES = ThreadLocal.withInitial(IntArrayList::new);

    // ------------------------------------------------------------- //

    public static void drawTooltip(final GuiGraphics graphics, final List<? extends FormattedText> tooltip, final int x, final int y) {
        drawTooltip(graphics, tooltip, x, y, 200, ItemStack.EMPTY);
    }

    public static void drawTooltip(final GuiGraphics graphics, final List<? extends FormattedText> tooltip, final int x, final int y, final int widthHint) {
        drawTooltip(graphics, tooltip, x, y, widthHint, ItemStack.EMPTY);
    }

    public static void drawTooltip(final GuiGraphics graphics, final List<? extends FormattedText> tooltip, final int x, final int y, final int widthHint, final ItemStack itemStack) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen screen = minecraft.screen;
        if (screen == null) {
            return;
        }

        final int availableWidth = Math.max(x, screen.width - x);
        final int targetWidth = Math.min(availableWidth, widthHint);
        final Font font = minecraft.font;

        final StringSplitter splitter = font.getSplitter();
        final boolean needsWrapping = tooltip.stream().anyMatch(line -> font.width(line) > targetWidth);
        final List<? extends FormattedText> lines = needsWrapping
            ? tooltip.stream().flatMap(line -> splitter.splitLines(line, targetWidth, Style.EMPTY).stream()).toList()
            : tooltip;
        graphics.renderTooltip(font, lines.stream().map(Language.getInstance()::getVisualOrder).toList(), x, y);
    }

    public static void tryAddDescription(final ItemStack stack, final List<Component> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        final String translationKey = stack.getDescriptionId() + Constants.TOOLTIP_DESCRIPTION_SUFFIX;
        final Language language = Language.getInstance();
        if (language.has(translationKey)) {
            final MutableComponent description = Component.translatable(translationKey);
            tooltip.add(withFormat(description, ChatFormatting.GRAY));
        }

        if (stack.is(ItemTags.DEVICE_NEEDS_REBOOT)) {
            tooltip.add(DEVICE_NEEDS_REBOOT);
        }

        final int energyConsumption;
        if (stack.getItem() instanceof BlockItem blockItem &&
            blockItem.getBlock() instanceof EnergyConsumingBlock energyConsumingBlock) {
            energyConsumption = energyConsumingBlock.getEnergyConsumption();
        } else {
            final ItemDeviceQuery query = Devices.makeQuery(stack);
            energyConsumption = Devices.getEnergyConsumption(query);
        }

        if (energyConsumption > 0) {
            final MutableComponent energy = withFormat(String.valueOf(energyConsumption), ChatFormatting.GREEN);
            tooltip.add(withFormat(Component.translatable(Constants.TOOLTIP_ENERGY_CONSUMPTION, energy), ChatFormatting.GRAY));
        }
    }

    public static void addBlockEntityInventoryInformation(final ItemStack stack, final List<Component> tooltip) {
        addInventoryInformation(NBTUtils.getChildTag(ItemStackUtils.getBlockEntityDataTag(stack), ITEMS_TAG_NAME), tooltip);
    }

    public static void addEntityInventoryInformation(final ItemStack stack, final List<Component> tooltip) {
        addInventoryInformation(NBTUtils.getChildTag(ItemStackUtils.getModDataTag(stack), ITEMS_TAG_NAME), tooltip);
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
            tooltip.add(Component.literal("- ")
                .append(itemStack.getDisplayName())
                .withStyle(style -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY)))
                .append(Component.literal(" x")
                    .append(String.valueOf(itemStackSizes.getInt(i)))
                    .withStyle(style -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY))))
            );
        }
    }

    public static void addEntityEnergyInformation(final ItemStack stack, final List<Component> tooltip) {
        final EnergyStorage energy = Capabilities.get(stack, Capabilities.ENERGY_STORAGE);
        if (energy == null || energy.getEnergyStored() == 0) {
            return;
        }

        final MutableComponent value = withFormat(energy.getEnergyStored() + "/" + energy.getMaxEnergyStored(), ChatFormatting.GREEN);
        tooltip.add(withFormat(Component.translatable(Constants.TOOLTIP_ENERGY, value), ChatFormatting.GRAY));
    }

    public static void addEnergyConsumption(final double value, final List<Component> tooltip) {
        if (value > 0) {
            tooltip.add(withFormat(Component.translatable(Constants.TOOLTIP_ENERGY_CONSUMPTION, withFormat(new DecimalFormat("#.##").format(value), ChatFormatting.GREEN)), ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------- //

    private static String[] getDeviceTypeNames() {
        return StreamSupport.stream(DeviceTypes.DEVICE_TYPE_REGISTRY.spliterator(), false)
            .map(DeviceTypes::key)
            .toArray(String[]::new);
    }

    private static void collectItemStacks(final CompoundTag tag, final List<ItemStack> stacks, final IntList stackSizes) {
        final HolderLookup.Provider registries = requireNonNull(Minecraft.getInstance().level).registryAccess();
        final ListTag itemsTag = tag.getList("Items", NBTTagIds.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            final CompoundTag itemTag = itemsTag.getCompound(i);
            final ItemStack itemStack = ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY);
            if (itemStack.isEmpty()) {
                continue;
            }

            boolean didMerge = false;
            for (int j = 0; j < stacks.size(); j++) {
                final ItemStack existingStack = stacks.get(j);
                if (ItemStack.isSameItemSameComponents(existingStack, itemStack)) {
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
