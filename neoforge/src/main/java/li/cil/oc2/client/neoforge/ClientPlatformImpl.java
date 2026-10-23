/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class ClientPlatformImpl {
    private static boolean isHotbarVisible = true;

    public static void setHotbarVisible(final boolean value) {
        isHotbarVisible = value;
    }

    public static boolean isHotbarVisible() {
        return isHotbarVisible;
    }

    @Nullable
    public static BlockEntityRenderState tryExtractBlockEntityRenderState(final BlockEntityRenderDispatcher dispatcher, final BlockEntity blockEntity, final float partialTicks, final Frustum frustum) {
        return dispatcher.tryExtractRenderState(blockEntity, partialTicks, null, frustum);
    }

    private ClientPlatformImpl() {
    }
}
