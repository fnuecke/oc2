package li.cil.oc2.client.render.font;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

public final class Bdf2Png {
    private static final String LOCATION_FONT_NORMAL = "modules/oc2/src/main/resources/assets/oc2/fonts/terminus/ter-u16n.bdf";
    private static final String LOCATION_FONT_BOLD = "modules/oc2/src/main/resources/assets/oc2/fonts/terminus/ter-u16b.bdf";

    public static void main(final String[] args) throws IOException {
        bdf2png(LOCATION_FONT_NORMAL);
        bdf2png(LOCATION_FONT_BOLD);
    }

    private static void bdf2png(final String path) throws IOException {
        final int[] bounds = new int[4];
        try (final InputStream inputStream = new FileInputStream(path)) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            expectLine(readHeader(reader, bounds), "CHARS ");

            if (bounds[0] <= 0 || bounds[1] <= 0) {
                throw new IOException();
            }

            final BufferedImage image = new BufferedImage(bounds[0] * 16, bounds[1] * 16, BufferedImage.TYPE_INT_ARGB);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ENDFONT")) {
                    ImageIO.write(image, "png", new File(path + ".png"));
                    return;
                }
                readChar(reader, image, bounds);
            }
        }
        throw new IOException();
    }

    private static void readChar(final BufferedReader reader, final BufferedImage image, final int[] defaultBounds) throws IOException {
        final int encoding = Integer.parseInt(expectLine(reader, "ENCODING "));
        expectLine(reader, "SWIDTH ");
        expectLine(reader, "DWIDTH ");
        final int[] bounds = expectBounds(expectLine(reader, "BBX "), new int[4]);
        if (!Arrays.equals(bounds, defaultBounds)) throw new IOException();

        expectLine(reader, "BITMAP");
        String line;
        int row = 0;
        while ((line = reader.readLine()) != null && !line.equals("ENDCHAR")) {
            final int rowBitmap = Integer.parseInt(line, 16);
            if (encoding < 0 || encoding >= 256) {
                continue;
            }

            final int x = (encoding % 16) * defaultBounds[0];
            final int y = (encoding / 16) * defaultBounds[1] + row++;

            for (int i = 1 << defaultBounds[0], j = 0; i > 0; i = i >>> 1, j++) {
                if ((rowBitmap & i) != 0) {
                    image.setRGB(x + j, y, 0xFFFFFFFF);
                }
            }
        }
    }

    @Nullable
    private static String readHeader(final BufferedReader reader, final int[] bounds) throws IOException {
        expectLine(reader, "STARTFONT ");
        expectLine(reader, "FONT ");
        expectLine(reader, "SIZE ");

        expectBounds(expectLine(reader, "FONTBOUNDINGBOX "), bounds);

        // Skip properties.
        String line;
        do {
            line = reader.readLine();
        } while ((line != null && !line.startsWith("CHARS ")));
        return line;
    }

    private static int[] expectBounds(final String boundsSpec, final int[] bounds) throws IOException {
        final String[] boundsSpecParts = boundsSpec.split(" ", 4);
        if (boundsSpecParts.length != 4) throw new IOException();
        for (int i = 0; i < boundsSpecParts.length; i++) {
            try {
                bounds[i] = Integer.parseInt(boundsSpecParts[i]);
            } catch (final NumberFormatException e) {
                throw new IOException();
            }
        }
        return bounds;
    }

    private static String expectLine(final BufferedReader reader, final String prefix) throws IOException {
        return expectLine(reader.readLine(), prefix);
    }

    private static String expectLine(@Nullable final String line, final String prefix) throws IOException {
        if (line == null || !line.startsWith(prefix)) throw new IOException();
        return line.substring(prefix.length());
    }
}
