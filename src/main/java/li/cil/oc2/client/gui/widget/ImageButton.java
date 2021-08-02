package li.cil.oc2.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.gui.GuiUtils;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public abstract class ImageButton extends AbstractButton {
    private static final long PRESS_DURATION = 200;
    private static final long TOOLTIP_DELAY = 250;

    ///////////////////////////////////////////////////////////////////

    private final Screen parent;
    private final List<? extends ITextComponent> tooltip;
    private final Sprite baseImage;
    private final Sprite pressedImage;
    private long lastPressedAt;
    private long hoveringStartedAt;

    ///////////////////////////////////////////////////////////////////

    public ImageButton(final Screen parent,
                       final int x, final int y,
                       final int width, final int height,
                       final ITextComponent caption,
                       @Nullable final ITextComponent description,
                       final Sprite baseImage,
                       final Sprite pressedImage) {
        super(x, y, width, height, caption);
        this.parent = parent;
        if (description == null) {
            this.tooltip = singletonList(caption);
        } else {
            this.tooltip = asList(caption, new StringTextComponent("").withStyle(style -> style.withColor(Color.fromLegacyFormat(TextFormatting.GRAY))).append(description));
        }
        this.baseImage = baseImage;
        this.pressedImage = pressedImage;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void onPress() {
        lastPressedAt = System.currentTimeMillis();
    }

    @Override
    public void renderButton(final MatrixStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        Sprite background = baseImage;
        if ((System.currentTimeMillis() - lastPressedAt) < PRESS_DURATION) {
            background = pressedImage;
        }

        background.draw(stack, x, y);

        if (isHovered()) {
            if (hoveringStartedAt == 0) {
                hoveringStartedAt = System.currentTimeMillis();
            }

            if ((System.currentTimeMillis() - hoveringStartedAt) > TOOLTIP_DELAY) {
                renderToolTip(stack, mouseX, mouseY);
            }
        } else {
            hoveringStartedAt = 0;
        }
    }

    @Override
    public void renderToolTip(final MatrixStack stack, final int mouseX, final int mouseY) {
        GuiUtils.drawHoveringText(stack, tooltip, mouseX, mouseY, parent.width, parent.height, 200, Minecraft.getInstance().font);
    }
}
