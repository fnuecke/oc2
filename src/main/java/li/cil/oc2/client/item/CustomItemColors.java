package li.cil.oc2.client.item;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public final class CustomItemColors {
    public static final int BLACK = 0xFF404040;
    public static final int GREY = 0xFF555555;
    public static final int LIGHT_GREY = 0xFF898989;
    public static final int WHITE = 0xFFCACACA;

    public static final int LIME = 0xFF65DA2B;
    public static final int GREEN = 0xFF1C9C31;
    public static final int CYAN = 0xFF11C5BD;
    public static final int BLUE = 0xFF4F66E8;
    public static final int LIGHT_BLUE = 0xFF1192C5;
    public static final int PURPLE = 0xFF8F02CA;
    public static final int MAGENTA = 0xFFC61087;
    public static final int PINK = 0xFFDB51BD;
    public static final int ORANGE = 0xFFDD803D;
    public static final int RED = 0xFFDD3D3D;
    public static final int BROWN = 0xFF745C42;
    public static final int YELLOW = 0xFFFFFC49;

    ///////////////////////////////////////////////////////////////////

    private static final String COLOR_TAG_NAME = "color";
    private static final int NO_TINT = 0xFFFFFFFF;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        final ItemColors itemColors = Minecraft.getInstance().getItemColors();
        itemColors.register((stack, layer) -> layer == 1 ? getColor(stack) : NO_TINT, Items.FLOPPY.get());
    }

    public static int getColorByDye(final DyeColor dye) {
        switch (dye) {
            case WHITE:
                return WHITE;
            case ORANGE:
                return ORANGE;
            case MAGENTA:
                return MAGENTA;
            case LIGHT_BLUE:
                return LIGHT_BLUE;
            case YELLOW:
                return YELLOW;
            case LIME:
                return LIME;
            case PINK:
                return PINK;
            case GRAY:
                return GREY;
            case LIGHT_GRAY:
                return LIGHT_GREY;
            case CYAN:
                return CYAN;
            case PURPLE:
                return PURPLE;
            case BLUE:
                return BLUE;
            case BROWN:
                return BROWN;
            case GREEN:
                return GREEN;
            case RED:
                return RED;
            case BLACK:
                return BLACK;
        }

        return GREY;
    }

    public static int getColor(final ItemStack stack) {
        final CompoundNBT tag = ItemStackUtils.getModDataTag(stack);
        if (tag != null && tag.contains(COLOR_TAG_NAME, NBTTagIds.TAG_INT)) {
            return tag.getInt(COLOR_TAG_NAME);
        } else {
            return GREY;
        }
    }

    public static ItemStack withColor(final ItemStack stack, final int color) {
        ItemStackUtils.getOrCreateModDataTag(stack).putInt(COLOR_TAG_NAME, color);
        return stack;
    }
}
