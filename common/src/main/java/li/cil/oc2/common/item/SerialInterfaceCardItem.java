/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.client.gui.SerialInterfaceCardScreen;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;
import static li.cil.oc2.common.util.TranslationUtils.key;
import static li.cil.oc2.common.util.TranslationUtils.text;

public final class SerialInterfaceCardItem extends ModItem {
    public static final int MAX_ADDRESS = 254; // 255 is broadcast

    private static final String CONFIGURATION_TEXT = key("item.{mod}.serial_interface_card.configuration");
    private static final Component IS_CONFIGURED_TEXT = withFormat(text("item.{mod}.sided_device.is_configured"), ChatFormatting.GREEN);

    private static final String SIDE_CONFIGURATION_TAG_NAME = "sides";
    private static final String ADDRESS_TAG_NAME = "address";

    // --------------------------------------------------------------------- //

    public static void setSideConfiguration(final ItemStack stack, final Direction side, final boolean enabled) {
        final int index = side.get3DDataValue();

        ItemStackUtils.modifyModDataTag(stack, tag -> {
            final byte[] values;
            if (tag.contains(SIDE_CONFIGURATION_TAG_NAME, NBTTagIds.TAG_BYTE_ARRAY) &&
                tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME).length == Constants.BLOCK_FACE_COUNT) {
                values = tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME);
            } else {
                values = new byte[Constants.BLOCK_FACE_COUNT];
                Arrays.fill(values, (byte) 1);
            }

            values[index] = (byte) (enabled ? 1 : 0);

            boolean anyNonDefault = false;
            for (final byte value : values) {
                if (value == 0) {
                    anyNonDefault = true;
                    break;
                }
            }

            if (anyNonDefault) {
                tag.putByteArray(SIDE_CONFIGURATION_TAG_NAME, values);
            } else {
                tag.remove(SIDE_CONFIGURATION_TAG_NAME);
            }
        });
    }

    public static boolean hasSideConfiguration(final ItemStack stack) {
        return ItemStackUtils.getModDataTag(stack).contains(SIDE_CONFIGURATION_TAG_NAME);
    }

    public static boolean getSideConfiguration(final ItemStack stack, @Nullable final Direction side) {
        if (side == null) {
            return false;
        }

        final int index = side.get3DDataValue();

        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (tag.contains(SIDE_CONFIGURATION_TAG_NAME, NBTTagIds.TAG_BYTE_ARRAY)) {
            final byte[] values = tag.getByteArray(SIDE_CONFIGURATION_TAG_NAME);
            if (index < values.length) {
                return values[index] != 0;
            }
        }

        return true;
    }

    public static boolean hasAddress(final ItemStack stack) {
        return ItemStackUtils.getModDataTag(stack).contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_INT);
    }

    public static int getAddress(final ItemStack stack) {
        final CompoundTag tag = ItemStackUtils.getModDataTag(stack);
        if (tag.contains(ADDRESS_TAG_NAME, NBTTagIds.TAG_INT)) {
            return Math.clamp(tag.getInt(ADDRESS_TAG_NAME), 0, MAX_ADDRESS);
        }

        return 0;
    }

    public static void setAddress(final ItemStack stack, final int address) {
        ItemStackUtils.modifyModDataTag(stack, tag ->
            tag.putInt(ADDRESS_TAG_NAME, Math.clamp(address, 0, MAX_ADDRESS)));
    }

    // --------------------------------------------------------------------- //

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(CONFIGURATION_TEXT,
            withFormat(Integer.toString(getAddress(stack)), ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY));
        if (hasSideConfiguration(stack)) {
            tooltip.add(IS_CONFIGURED_TEXT);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack itemStack = player.getItemInHand(hand);

        if (player.level().isClientSide()) {
            if (itemStack.is(Items.SERIAL_INTERFACE_CARD.get())) {
                openConfigurationScreen(player, hand);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, player.level().isClientSide());
    }

    // --------------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    private void openConfigurationScreen(final Player player, final InteractionHand hand) {
        Minecraft.getInstance().setScreen(new SerialInterfaceCardScreen(player, hand));
    }
}
