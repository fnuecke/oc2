/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import li.cil.oc2.common.vm.Terminal;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TerminalFontTests {
    private static final String ATLAS = "/assets/oc2/textures/font/terminus.png";
    private static final int COLUMNS = 16, BOLD_SHIFT = 16;
    private static final int WHITE_CELL = 0x7F;

    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void everyGlyphInTheAtlasIsDrawn() {
        final BufferedImage atlas = atlas();

        for (int character = 0; character < 256; character++) {
            if (character == WHITE_CELL) {
                continue; // Reserved as an untextured source, never drawn as a glyph.
            }

            final boolean hasGlyph = !isBlank(atlas, character, false) || !isBlank(atlas, character, true);
            assertEquals(hasGlyph, TerminalRenderer.isPrintableCharacter((char) character),
                "cell 0x%02X is %s in the font".formatted(character, hasGlyph ? "drawn" : "blank"));
        }
    }

    @Test
    public void lineDrawingGlyphsAreDrawn() {
        final BufferedImage atlas = atlas();

        for (char ch = 0x5F; ch <= 0x7E; ch++) {
            final int cell = ch - 0x5F;
            if (ch == '_') {
                continue; // The blank of the line drawing set.
            }

            assertTrue(TerminalRenderer.isPrintableCharacter((char) cell), "line drawing '" + ch + "' is not drawn");
            assertTrue(!isBlank(atlas, cell, false) && !isBlank(atlas, cell, true), "line drawing '" + ch + "' has no glyph");
        }
    }

    @Test
    public void theUntexturedSourceIsOpaque() {
        final BufferedImage atlas = atlas();
        final int resolution = atlas.getWidth();
        final int x = Math.round(readFloat("WHITE_U") * resolution);
        final int y = Math.round(readFloat("WHITE_V") * resolution);

        assertEquals(0xFF, atlas.getRGB(x, y) >>> 24,
            "backgrounds and underlines sample this texel; a transparent one discards them");
    }

    // --------------------------------------------------------------------- //

    private static boolean isBlank(final BufferedImage atlas, final int character, final boolean isBold) {
        final int width = atlas.getWidth() / (COLUMNS * 2);
        final int height = atlas.getHeight() / COLUMNS;
        final int x = (character % COLUMNS + (isBold ? BOLD_SHIFT : 0)) * width;
        final int y = character / COLUMNS * height;

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if ((atlas.getRGB(x + column, y + row) >>> 24) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BufferedImage atlas() {
        try (InputStream stream = TerminalFontTests.class.getResourceAsStream(ATLAS)) {
            assertNotNull(stream, "font atlas is missing from the resources");
            final BufferedImage image = ImageIO.read(stream);
            assertEquals(Terminal.CHAR_WIDTH * COLUMNS * 2, image.getWidth());
            return image;
        } catch (final IOException e) {
            throw new AssertionError("could not read the font atlas", e);
        }
    }

    private static float readFloat(final String name) {
        try {
            final Field field = TerminalRenderer.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getFloat(null);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("could not read " + name, e);
        }
    }
}
