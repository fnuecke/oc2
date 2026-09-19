/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.blockentity.ModBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class AbstractBlockEntityScreen<T extends ModBlockEntity> extends Screen {
    private static final int USE_DISTANCE = 8;
    private static final int DEFAULT_WIDTH = 176;
    private static final int DEFAULT_HEIGHT = 166;

    protected static final int CONTROLS_TOP = 8;

    // --------------------------------------------------------------------- //

    protected final T blockEntity;

    protected int imageWidth = DEFAULT_WIDTH;
    protected int imageHeight = DEFAULT_HEIGHT;
    protected int leftPos, topPos;

    // --------------------------------------------------------------------- //

    protected AbstractBlockEntityScreen(Component title, T blockEntity) {
        super(title);
        this.blockEntity = blockEntity;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void init() {
        super.init();

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
    }

    @Override
    public void tick() {
        super.tick();

        if (stillValid()) {
            safeTick();
        } else {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderTransparentBackground(graphics);
        renderBg(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderFg(graphics, mouseX, mouseY, partialTicks);
    }

    // --------------------------------------------------------------------- //

    protected void safeTick() {
    }

    protected void renderBg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
    }

    protected void renderFg(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
    }

    // --------------------------------------------------------------------- //

    private boolean stillValid() {
        final Vec3 blockCenter = Vec3.atCenterOf(blockEntity.getBlockPos());
        return blockEntity.isValid() &&
            minecraft.player != null &&
            minecraft.player.distanceToSqr(blockCenter) <= USE_DISTANCE * USE_DISTANCE;
    }
}
