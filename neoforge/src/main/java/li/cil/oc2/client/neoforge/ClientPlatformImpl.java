/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ClientPlatformImpl {
    private static boolean isHotbarVisible = true;

    public static void setHotbarVisible(final boolean value) {
        isHotbarVisible = value;
    }

    public static boolean isHotbarVisible() {
        return isHotbarVisible;
    }

    public static boolean isStencilEnabled(final RenderTarget target) {
        return target.isStencilEnabled();
    }

    public static void enableStencil(final RenderTarget target) {
        target.enableStencil();
    }

    public static void registerItemProperty(final Item item, final ResourceLocation name, final ClampedItemPropertyFunction property) {
        ItemProperties.register(item, name, property);
    }

    private ClientPlatformImpl() {
    }
}
