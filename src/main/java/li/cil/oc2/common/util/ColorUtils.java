package li.cil.oc2.common.util;

public final class ColorUtils {
    public static int textureDiffuseColorsToRGB(final float[] colors) {
        final int r = ((int) colors[0] * 255) & 0xFF;
        final int g = ((int) colors[1] * 255) & 0xFF;
        final int b = ((int) colors[2] * 255) & 0xFF;
        return r << 16 | g << 8 | b;
    }
}
