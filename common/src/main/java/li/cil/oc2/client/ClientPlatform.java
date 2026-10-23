/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class ClientPlatform {
    @ExpectPlatform
    public static void setHotbarVisible(final boolean value) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static BlockEntityRenderState tryExtractBlockEntityRenderState(final BlockEntityRenderDispatcher dispatcher, final BlockEntity blockEntity, final float partialTicks, final Frustum frustum) {
        throw new AssertionError();
    }

    private ClientPlatform() {
    }
}
