/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.block;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.UnaryOperator;

@RPCDeviceDescription(typeName = "sign", description = """
    Provided by signs connected to a [bus interface](../block/bus_interface.md). Signs can have text on their `front` and `back`.""")
@IODeviceDescription(name = "SIGN", description = """
    Sides are numbered: `0` for front, `1` for back.

    Colors are numbered as dyes: `0` white, `1` orange, `2` magenta, `3` light blue, `4` yellow, `5` lime, `6` pink, `7` gray, `8` light gray, `9` cyan, `10` purple, `11` blue, `12` brown, `13` green, `14` red, `15` black.""")
public final class SignDevice extends AbstractBlockDevice {
    private static final int GET_LINE_CODE = 1;
    private static final int SET_LINE_CODE = 2;
    private static final int GET_COLOR_CODE = 3;
    private static final int IS_GLOWING_CODE = 4;
    private static final int IS_READONLY_CODE = 5;

    private static final int WIDEST_GLYPH_STEP = 7;

    // --------------------------------------------------------------------- //

    public SignDevice(final BlockLocation identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets the text on a side of the sign.",
        returnValueDescription = "the current text lines.")
    public String[] getText(@Nullable @Parameter(value = "side", description = "`front` or `back`.") final String side) {
        final SignText text = getSign().getText(isFront(side));
        final String[] lines = new String[SignText.LINES];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = text.getMessage(i, false).getString();
        }
        return lines;
    }

    @Callback(description = "Sets the text on a side of the sign.")
    public void setText(@Nullable @Parameter(value = "side", description = "`front` or `back`.") final String side,
                        @Nullable @Parameter(value = "lines", description = "up to four lines of text.") final String[] lines) {
        if (lines == null || lines.length > SignText.LINES) {
            throw new IllegalArgumentException("expected up to " + SignText.LINES + " lines");
        }

        final Component[] messages = new Component[SignText.LINES];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = toMessage(i < lines.length ? lines[i] : "");
        }

        updateText(isFront(side), text -> {
            SignText result = text;
            for (int i = 0; i < messages.length; i++) {
                result = result.setMessage(i, messages[i]);
            }
            return result;
        });
    }

    @Callback(description = "Gets the color of the text on a side of the sign.",
        returnValueDescription = "the name of the color.")
    public String getColor(@Nullable @Parameter(value = "side", description = "`front` or `back`.") final String side) {
        return getSign().getText(isFront(side)).getColor().getName();
    }

    @Callback(description = "Gets whether the text on a side of the sign glows.",
        returnValueDescription = "`true` if the text glows.")
    public boolean isGlowing(@Nullable @Parameter(value = "side", description = "`front` or `back`.") final String side) {
        return getSign().getText(isFront(side)).hasGlowingText();
    }

    @Callback(description = "Gets whether the sign is read-only.",
        returnValueDescription = "whether the sign is read-only.")
    public boolean isReadonly() {
        return getSign().isWaxed();
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_LINE_CODE,
        description = "Gets a line of text on a side of the sign.",
        argumentsDescription = "two bytes, the side, `0` for front, `1` for back, and the line.",
        resultsDescription = "the text. Read while `OCDAV` is set to read fully.")
    public void getLine(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final boolean front = isFront(arguments.readU8());
        final int line = requireValidLine(arguments.readU8());
        final String text = getSign().getText(front).getMessage(line, false).getString();
        final StringBuilder printable = new StringBuilder();
        for (int i = 0; i < text.length() && printable.length() < IOCallback.MAX_DATA_SIZE; i++) {
            final char c = text.charAt(i);
            printable.append(c >= ' ' && c <= '~' ? c : '?');
        }
        results.writeString(printable.toString());
    }

    @IOCallback(value = SET_LINE_CODE,
        description = "Sets a line of text on a side of the sign.",
        argumentsDescription = "the side, `0` for front, `1` for back, and the line as one byte each, then the text.")
    public void setLine(final IOInputStream arguments) throws IOException {
        final boolean front = isFront(arguments.readU8());
        final int line = requireValidLine(arguments.readU8());
        final Component message = toMessage(arguments.readString());
        updateText(front, text -> text.setMessage(line, message));
    }

    @IOCallback(value = GET_COLOR_CODE,
        description = "Gets the color of the text on a side of the sign.",
        argumentsDescription = "one byte, `0` for front, `1` for back.",
        resultsDescription = "one byte, the color code.")
    public void getColor(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getSign().getText(isFront(arguments.readU8())).getColor().getId());
    }

    @IOCallback(value = IS_GLOWING_CODE,
        description = "Gets whether the text on a side of the sign glows.",
        argumentsDescription = "one byte, `0` for front, `1` for back.",
        resultsDescription = "one byte, `1` if the text glows, `0` otherwise.")
    public void isGlowing(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(getSign().getText(isFront(arguments.readU8())).hasGlowingText() ? 1 : 0);
    }

    @IOCallback(value = IS_READONLY_CODE,
        description = "Gets whether the sign is read-only.",
        resultsDescription = "one byte, `1` if read-only, `0` otherwise.")
    public void isReadonly(final IOOutputStream results) throws IOException {
        results.writeU8(isReadonly() ? 1 : 0);
    }

    // --------------------------------------------------------------------- //

    private static boolean isFront(@Nullable final String side) {
        if ("front".equals(side)) return true;
        if ("back".equals(side)) return false;
        throw new IllegalArgumentException("side must be front or back");
    }

    private static boolean isFront(final int side) {
        if (side == 0) return true;
        if (side == 1) return false;
        throw new IllegalArgumentException("side must be 0 or 1");
    }

    private static int requireValidLine(final int line) {
        if (line >= SignText.LINES) {
            throw new IllegalArgumentException("line must be 0 to " + (SignText.LINES - 1));
        }
        return line;
    }

    private Component toMessage(@Nullable final String line) {
        if (line == null) {
            throw new IllegalArgumentException("line must be text");
        }
        if (line.length() > getMaxLineLength()) {
            throw new IllegalArgumentException("line is longer than " + getMaxLineLength() + " characters: " + line);
        }
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c < ' ' || c > '~') {
                throw new IllegalArgumentException("line contains a character other than printable ASCII: " + line);
            }
        }
        return Component.literal(line);
    }

    private int getMaxLineLength() {
        return getSign().getMaxTextLineWidth() / WIDEST_GLYPH_STEP;
    }

    private void updateText(final boolean front, final UnaryOperator<SignText> updater) {
        final SignBlockEntity sign = getSign();
        if (sign.isWaxed()) {
            throw new IllegalStateException("sign is read-only");
        }

        checkPermission();
        sign.updateText(updater, front);
    }

    private SignBlockEntity getSign() {
        return getBlockEntity(SignBlockEntity.class);
    }
}
