/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import net.minecraft.network.chat.CommonComponents;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

public abstract class ImageButton extends AbstractButton {
    private static final long PRESS_DURATION = 200;
    private static final long TOOLTIP_DELAY = 250;

    ///////////////////////////////////////////////////////////////////

    private final Sprite baseImage;
    private final Sprite pressedImage;
    private List<Component> tooltip = emptyList();
    private long lastPressedAt;
    private long hoveringStartedAt;

    ///////////////////////////////////////////////////////////////////

    protected ImageButton(final int x, final int y, final int width, final int height, final Sprite baseImage, final Sprite pressedImage) {
        super(x, y, width, height, CommonComponents.EMPTY);
        this.baseImage = baseImage;
        this.pressedImage = pressedImage;
    }

    ///////////////////////////////////////////////////////////////////

    public ImageButton withMessage(final Component component) {
        setMessage(component);
        return this;
    }

    public ImageButton withTooltip(final Component... components) {
        tooltip = Arrays.asList(components);
        for (int i = 1; i < tooltip.size(); i++) {
            final Component component = tooltip.get(i);
            tooltip.set(i, withFormat(component, ChatFormatting.GRAY));
        }
        return this;
    }

    @Override
    public void onPress() {
        lastPressedAt = System.currentTimeMillis();
    }

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);

        renderTooltipIfHovered(graphics, mouseX, mouseY);
    }

    // AbstractWidget.renderToolTip is gone in 1.21.1; the button draws its own tooltip.
    private void renderTooltipIfHovered(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (tooltip.isEmpty()) {
            return;
        }

        if (isHoveredOrFocused()) {
            if (hoveringStartedAt == 0) {
                hoveringStartedAt = System.currentTimeMillis();
            }

            if ((System.currentTimeMillis() - hoveringStartedAt) > TOOLTIP_DELAY) {
                TooltipUtils.drawTooltip(graphics, tooltip, mouseX, mouseY, 200);
            }
        } else {
            hoveringStartedAt = 0;
        }
    }

    @Override
    public void updateNarration(final NarrationElementOutput element) {
        this.defaultButtonNarrationText(element);
    }

    ///////////////////////////////////////////////////////////////////

    protected void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        RenderSystem.enableDepthTest();

        Sprite background = baseImage;
        if ((System.currentTimeMillis() - lastPressedAt) < PRESS_DURATION) {
            background = pressedImage;
        }

        background.draw(graphics, x, y);

        if (!Objects.equals(getMessage(), CommonComponents.EMPTY)) {
            drawCenteredString(graphics, Minecraft.getInstance().font, getMessage(),
                x + width / 2, y + (height - 8) / 2,
                getFGColor() | Mth.ceil(alpha * 255) << 24);
        }
    }
}
