package li.cil.oc2.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ImageButton extends AbstractButton {
    private static final long PRESS_DURATION = 200;
    private static final long TOOLTIP_DELAY = 250;

    private final Screen parent;
    private final List<? extends Component> tooltip;
    private final Sprite baseImage;
    private final Sprite pressedImage;
    private long lastPressedAt;
    private long hoveringStartedAt;

    public ImageButton(final Screen parent,
                       final int x, final int y,
                       final int width, final int height,
                       final Component caption,
                       @Nullable final Component description,
                       final Sprite baseImage,
                       final Sprite pressedImage) {
        super(x, y, width, height, caption);
        this.parent = parent;
        if (description == null) {
            this.tooltip = Collections.singletonList(caption);
        } else {
            this.tooltip = Arrays.asList(caption, new TextComponent("").withStyle(style -> style.withColor(ChatFormatting.GRAY)).append(description));
        }
        this.baseImage = baseImage;
        this.pressedImage = pressedImage;
    }

    @Override
    public void onPress() {
        lastPressedAt = System.currentTimeMillis();
    }

    @Override
    public void renderButton(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
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
    public void renderToolTip(final PoseStack stack, final int mouseX, final int mouseY) {
        GuiUtils.drawHoveringText(stack, tooltip, mouseX, mouseY, parent.width, parent.height, 200, Minecraft.getInstance().font);
    }
}
