/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import li.cil.oc2.client.gui.SideConfigurationBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static li.cil.oc2.common.util.TranslationUtils.key;
import static li.cil.oc2.common.util.TranslationUtils.text;

public final class SideConfigurationWidget extends AbstractWidget {
    private static final String SIDE_STATE_TEXT = key("gui.{mod}.sided_device.side_state");
    private static final Component CONNECTIVITY_ENABLED_TEXT = text("gui.{mod}.sided_device.connectivity.enabled");
    private static final Component CONNECTIVITY_DISABLED_TEXT = text("gui.{mod}.sided_device.connectivity.disabled");
    private static final int MAX_BLOCK_PITCH = 30;
    private static final double DRAG_THRESHOLD = 3;
    private static final int SIZE = 100;

    // --------------------------------------------------------------------- //

    private final Predicate<Direction> getConfiguration;
    private final BiConsumer<Direction, Boolean> sendConfiguration;
    private final ComputerBlockItemRenderer computerBlockItemRenderer = new ComputerBlockItemRenderer();

    private Vector3f blockRotation = new Vector3f(-30, 45, 0);
    @Nullable
    private Direction focusedSide;
    private boolean isPressed, hasDraggedBlock;
    private double dragStartX, dragStartY;

    // --------------------------------------------------------------------- //

    public SideConfigurationWidget(final Predicate<Direction> getConfiguration, final BiConsumer<Direction, Boolean> sendConfiguration) {
        super(0, 0, SIZE, SIZE, CommonComponents.EMPTY);
        this.getConfiguration = getConfiguration;
        this.sendConfiguration = sendConfiguration;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void onClick(final MouseButtonEvent event, final boolean isDoubleClick) {
        isPressed = true;
        hasDraggedBlock = false;
        dragStartX = event.x();
        dragStartY = event.y();
    }

    @Override
    public void onRelease(final MouseButtonEvent event) {
        if (isPressed && !hasDraggedBlock && focusedSide != null) {
            sendConfiguration.accept(focusedSide, !getConfiguration.test(focusedSide));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
        }
        isPressed = false;
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void onDrag(final MouseButtonEvent event, final double deltaX, final double deltaY) {
        if (!hasDraggedBlock) {
            final double dx = event.x() - dragStartX;
            final double dy = event.y() - dragStartY;
            hasDraggedBlock = Math.sqrt(dx * dx + dy * dy) > DRAG_THRESHOLD;
        }
        if (hasDraggedBlock) {
            blockRotation = new Vector3f(
                Mth.clamp(blockRotation.x() - (float) deltaY, -MAX_BLOCK_PITCH, MAX_BLOCK_PITCH),
                Mth.wrapDegrees(blockRotation.y() + (float) deltaX),
                blockRotation.z()
            );
        }
    }

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final int blockX = getX() + width / 2;
        final int blockY = getY() + height / 2;
        focusedSide = isHovered() ? computerBlockItemRenderer.getFocusedSide(blockX - mouseX, blockY - mouseY, blockRotation) : null;
        computerBlockItemRenderer.render(graphics, getX(), getY(), getRight(), getBottom(), blockRotation);

        if (focusedSide != null) {
            final Component enabledComponent = getConfiguration.test(focusedSide) ? CONNECTIVITY_ENABLED_TEXT : CONNECTIVITY_DISABLED_TEXT;
            graphics.setTooltipForNextFrame(Minecraft.getInstance().font, Component.translatable(SIDE_STATE_TEXT, enabledComponent), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }

    // --------------------------------------------------------------------- //

    private final class ComputerBlockItemRenderer {
        public static final int BLOCK_RENDER_SIZE = 48;

        @Nullable
        private Direction getFocusedSide(final float mouseX, final float mouseY, final Vector3f rotation) {
            // Rotate ray inversely around block to represent visual block rotation.
            final Quaternionf quaternion = fromXYZDegrees(toRenderRotation(rotation));
            quaternion.conjugate();

            // Move ray in screen space to mouse position.
            final float relMouseX = -mouseX / BLOCK_RENDER_SIZE;
            final float relMouseY = -mouseY / BLOCK_RENDER_SIZE;

            final Vector3f source = new Vector3f();
            source.add(relMouseX, relMouseY, 1);
            source.rotate(quaternion);

            final Vector3f target = new Vector3f();
            target.add(relMouseX, relMouseY, -1);
            target.rotate(quaternion);

            // Intersect rotated ray with bounding box representing block.
            final AABB aabb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            return aabb.clip(new Vec3(source), new Vec3(target))
                .map(hit -> Direction.getApproximateNearest(-hit.x, -hit.y(), -hit.z()))
                .filter(side -> side != Direction.SOUTH)
                .orElse(null);
        }

        public void render(final GuiGraphics graphics, final int x0, final int y0, final int x1, final int y1, final Vector3f rotation) {
            int enabledSides = 0;
            for (final Direction side : Direction.values()) {
                if (getConfiguration.test(side)) {
                    enabledSides |= 1 << side.get3DDataValue();
                }
            }

            graphics.guiRenderState.submitPicturesInPictureState(new SideConfigurationBlockRenderer.State(
                fromXYZDegrees(toRenderRotation(rotation)), enabledSides, focusedSide,
                x0, y0, x1, y1, BLOCK_RENDER_SIZE));
        }

        private static Vector3f toRenderRotation(final Vector3f rotation) {
            return new Vector3f(rotation).add(0, 180, 0);
        }

        private static Quaternionf fromXYZDegrees(final Vector3f degrees) {
            return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(degrees.x()),
                (float) Math.toRadians(degrees.y()),
                (float) Math.toRadians(degrees.z()));
        }
    }
}
