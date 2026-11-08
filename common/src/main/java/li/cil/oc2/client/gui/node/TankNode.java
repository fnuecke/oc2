/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import dev.architectury.hooks.fluid.FluidStackHooks;
import li.cil.oc2.client.gui.layout.Axis;
import li.cil.oc2.client.gui.layout.ProgressBarNode;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.RobotInventoryContainer;
import li.cil.oc2.common.fluid.FluidStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

import java.util.List;

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

@Environment(EnvType.CLIENT)
public final class TankNode extends ProgressBarNode {
    private static final int WIDTH = 54, HEIGHT = 8;
    private static final int FLUID_SPRITE_SIZE = 16;

    // --------------------------------------------------------------------- //

    public TankNode(final RobotInventoryContainer menu) {
        super(Axis.HORIZONTAL, () -> menu.getFluid().amount() / (double) menu.getFluidCapacity(), fluidFill(menu));
        tooltip(() -> {
            final FluidStack fluid = menu.getFluid();
            final Component name = fluid.isEmpty()
                ? Component.translatable(Constants.TOOLTIP_FLUID_EMPTY)
                : FluidStackHooks.getName(toArchitectury(fluid));
            return List.of(Component.translatable(Constants.TOOLTIP_FLUID, name,
                withFormat(fluid.amount() + "/" + menu.getFluidCapacity(), ChatFormatting.GREEN)));
        });
    }

    // --------------------------------------------------------------------- //
    // Node

    @Override
    protected int contentWidth() {
        return WIDTH;
    }

    @Override
    protected int contentHeight() {
        return HEIGHT;
    }

    // --------------------------------------------------------------------- //

    private static FillRenderer fluidFill(final RobotInventoryContainer menu) {
        return (renderer, x, y, width, height) -> {
            final FluidStack fluid = menu.getFluid();
            if (fluid.isEmpty()) {
                return;
            }

            final var stack = toArchitectury(fluid);
            final TextureAtlasSprite sprite = FluidStackHooks.getStillTexture(stack);
            if (sprite == null) {
                return;
            }

            // Animated atlas sprites don't tile, so draw whole sprites and let the bar cut them off.
            final int color = 0xFF000000 | FluidStackHooks.getColor(stack);
            for (int offset = 0; offset < width; offset += FLUID_SPRITE_SIZE) {
                renderer.atlasSprite(sprite, x + offset, y, FLUID_SPRITE_SIZE, FLUID_SPRITE_SIZE, color);
            }
        };
    }

    private static dev.architectury.fluid.FluidStack toArchitectury(final FluidStack fluid) {
        return dev.architectury.fluid.FluidStack.create(fluid.fluid(), fluid.amount(), fluid.components());
    }
}
