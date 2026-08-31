package li.cil.oc2.client.render.font;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Bdf2Png {
    private static final String FONT_REGULAR = "assets/fonts/ter-u16n.bdf";
    private static final String FONT_BOLD = "assets/fonts/ter-u16b.bdf";
    private static final String ATLAS = "common/src/main/resources/assets/oc2/textures/font/terminus.png";

    private static final int COLUMNS = 16, ROWS = 16;
    private static final int WHITE_CELL = 0x7F;

    private static final int[] SPECIAL_GRAPHICS = {
        0x0020, 0x25C6, 0x2592, 0x2409, 0x240C, 0x240D, 0x240A, 0x00B0,
        0x00B1, 0x2424, 0x240B, 0x2518, 0x2510, 0x250C, 0x2514, 0x253C,
        0x23BA, 0x23BB, 0x2500, 0x23BC, 0x23BD, 0x251C, 0x2524, 0x2534,
        0x252C, 0x2502, 0x2264, 0x2265, 0x03C0, 0x2260, 0x00A3, 0x00B7,
    };

    public static void main(final String[] args) throws IOException {
        final int[] bounds = new int[4];
        final Map<Integer, int[]> regular = readFont(FONT_REGULAR, bounds);
        final Map<Integer, int[]> bold = readFont(FONT_BOLD, new int[4]);

        final BufferedImage atlas = new BufferedImage(
            bounds[0] * COLUMNS * 2, bounds[1] * ROWS, BufferedImage.TYPE_INT_ARGB);
        draw(atlas, regular, 0, bounds);
        draw(atlas, bold, COLUMNS, bounds);
        fillWhiteCell(atlas, bounds);

        final File file = new File(ATLAS);
        ImageIO.write(atlas, "png", file);
        System.out.println("Wrote " + file.getAbsolutePath());
    }

    private static void draw(final BufferedImage atlas, final Map<Integer, int[]> glyphs, final int columnOffset, final int[] bounds) {
        for (int cell = 0; cell < COLUMNS * ROWS; cell++) {
            final int codePoint = cell < SPECIAL_GRAPHICS.length ? SPECIAL_GRAPHICS[cell] : cell;
            final int[] rows = glyphs.get(codePoint);
            if (rows == null) {
                continue;
            }

            final int x = (cell % COLUMNS + columnOffset) * bounds[0];
            final int y = (cell / COLUMNS) * bounds[1];
            for (int row = 0; row < rows.length; row++) {
                for (int column = 0; column < bounds[0]; column++) {
                    if ((rows[row] & (1 << (Integer.SIZE - 1 - column))) != 0) {
                        atlas.setRGB(x + column, y + row, 0xFFFFFFFF);
                    }
                }
            }
        }
    }

    private static void fillWhiteCell(final BufferedImage atlas, final int[] bounds) {
        final int x = (WHITE_CELL % COLUMNS + COLUMNS) * bounds[0];
        final int y = (WHITE_CELL / COLUMNS) * bounds[1];
        for (int row = 0; row < bounds[1]; row++) {
            for (int column = 0; column < bounds[0]; column++) {
                atlas.setRGB(x + column, y + row, 0xFFFFFFFF);
            }
        }
    }

    private static Map<Integer, int[]> readFont(final String path, final int[] bounds) throws IOException {
        try (final InputStream inputStream = new FileInputStream(path)) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            expectLine(readHeader(reader, bounds), "CHARS ");

            if (bounds[0] <= 0 || bounds[1] <= 0) {
                throw new IOException("Font has no usable bounding box.");
            }

            final Map<Integer, int[]> glyphs = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ENDFONT")) {
                    return glyphs;
                }
                readChar(reader, glyphs, bounds);
            }
        }
        throw new IOException("Font ended without ENDFONT.");
    }

    private static void readChar(final BufferedReader reader, final Map<Integer, int[]> glyphs, final int[] defaultBounds) throws IOException {
        final int encoding = Integer.parseInt(expectLine(reader, "ENCODING "));
        expectLine(reader, "SWIDTH ");
        expectLine(reader, "DWIDTH ");
        final int[] bounds = expectBounds(expectLine(reader, "BBX "), new int[4]);
        if (!Arrays.equals(bounds, defaultBounds)) throw new IOException("Glyph " + encoding + " has its own bounding box.");

        expectLine(reader, "BITMAP");
        final int[] rows = new int[defaultBounds[1]];
        String line;
        int row = 0;
        while ((line = reader.readLine()) != null && !line.equals("ENDCHAR")) {
            rows[row++] = Integer.parseUnsignedInt(line, 16) << (Integer.SIZE - line.length() * 4);
        }

        glyphs.put(encoding, rows);
    }

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

    private static String expectLine(final String line, final String prefix) throws IOException {
        if (line == null || !line.startsWith(prefix)) throw new IOException();
        return line.substring(prefix.length());
    }
}
