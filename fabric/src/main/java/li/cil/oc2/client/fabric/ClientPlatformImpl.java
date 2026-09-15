/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ClientPlatformImpl {
    private static boolean isHotbarVisible = true;

    public static void setHotbarVisible(final boolean value) {
        isHotbarVisible = value;
    }

    public static boolean isHotbarVisible() {
        return isHotbarVisible;
    }

    public static boolean isStencilEnabled(final RenderTarget ignoredTarget) {
        return false;
    }

    public static void enableStencil(final RenderTarget ignoredTarget) {
    }

    public static boolean isBlockEntityVisible(final BlockEntityRenderDispatcher ignoredDispatcher, final BlockEntity ignoredBlockEntity, final Frustum ignoredFrustum) {
        return true;
    }

    public static void registerItemProperty(final Item item, final ResourceLocation name, final ClampedItemPropertyFunction property) {
        ItemProperties.register(item, name, property);
    }

    private ClientPlatformImpl() {
    }
}
