package li.cil.oc2.common.util;

public final class TextFormatUtils {
    private static final int SIZE_STEP = 1024;
    private static final String[] SIZE_FORMAT = {"%dB", "%dKB", "%dMB", "%dGB", "%dTB"};

    public static String formatSize(int size) {
        int index = 0;
        while (size > SIZE_STEP && index < SIZE_FORMAT.length) {
            size /= SIZE_STEP;
            index++;
        }
        return String.format(SIZE_FORMAT[index], size);
    }
}
