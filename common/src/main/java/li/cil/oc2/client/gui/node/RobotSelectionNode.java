/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.layout.Node;
import li.cil.oc2.client.gui.layout.UiRenderer;
import li.cil.oc2.common.entity.Robot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;

import static li.cil.oc2.client.gui.layout.SlotNode.SLOT_SIZE;

@Environment(EnvType.CLIENT)
public final class RobotSelectionNode extends Node {
    private static final ResourceLocation SELECTION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot/selection");

    // --------------------------------------------------------------------- //

    private final Robot robot;
    private final int columns;

    // --------------------------------------------------------------------- //

    public RobotSelectionNode(final Robot robot, final int columns) {
        this.robot = robot;
        this.columns = columns;
        overlay();
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        final ScreenRectangle bounds = bounds();
        final int selected = robot.getSelectedSlot();
        renderer.sprite(SELECTION, bounds.left() + selected % columns * SLOT_SIZE, bounds.top() + selected / columns * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
