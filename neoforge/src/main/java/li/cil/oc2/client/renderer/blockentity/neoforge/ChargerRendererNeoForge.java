/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity.neoforge;

import li.cil.oc2.client.renderer.blockentity.ChargerRenderer;
import li.cil.oc2.common.blockentity.ChargerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public final class ChargerRendererNeoForge extends ChargerRenderer {
    public ChargerRendererNeoForge(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AABB getRenderBoundingBox(final ChargerBlockEntity blockEntity) {
        @Nullable final AABB expanded = blockEntity.getExpandedRenderBoundingBox();
        return expanded != null ? expanded : super.getRenderBoundingBox(blockEntity);
    }
}
