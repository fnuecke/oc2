/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderEventsNeoForge {
    private static final Matrix4f CAMERA_RELATIVE = new Matrix4f();

    // --------------------------------------------------------------------- //

    @Nullable
    private static Camera camera;
    @Nullable
    private static Frustum frustum;

    // --------------------------------------------------------------------- //

    @SubscribeEvent
    public static void handleExtractLevelRenderState(final ExtractLevelRenderStateEvent event) {
        camera = event.getCamera();
        frustum = event.getFrustum();
    }

    @SubscribeEvent
    public static void handleRenderAfterEntities(final RenderLevelStageEvent.AfterEntities event) {
        if (camera != null && frustum != null) {
            NetworkCableRenderer.render(camera, CAMERA_RELATIVE, frustum);
        }
    }

    @SubscribeEvent
    public static void handleRenderAfterTranslucentBlocks(final RenderLevelStageEvent.AfterTranslucentBlocks event) {
        BusInterfaceNameRenderer.INSTANCE.render(event.getPoseStack());
    }

    @SubscribeEvent
    public static void handleRenderGuiLayer(final RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.HOTBAR.equals(event.getName()) && !ClientPlatformImpl.isHotbarVisible()) {
            event.setCanceled(true);
        }
    }

    private ClientRenderEventsNeoForge() {
    }
}
