/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.model.fabric;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public final class EmissiveBakedModel extends ForwardingBakedModel {
    private final RenderMaterial material;

    // --------------------------------------------------------------------- //

    public EmissiveBakedModel(final BakedModel wrapped, final RenderMaterial material) {
        super(wrapped);
        this.material = material;
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(final BlockAndTintGetter level, final BlockState state, final BlockPos pos,
                               final Supplier<RandomSource> randomSupplier, final RenderContext context) {
        context.pushTransform(quad -> {
            quad.material(material);
            return true;
        });

        try {
            super.emitBlockQuads(level, state, pos, randomSupplier, context);
        } finally {
            context.popTransform();
        }
    }
}
