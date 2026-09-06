/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.util.TriState;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderEventsNeoForge {
    @SubscribeEvent
    public static void handleRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            NetworkCableRenderer.render(event.getCamera(), event.getModelViewMatrix(), event.getFrustum());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            BusInterfaceNameRenderer.INSTANCE.render(event.getPoseStack());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ProjectorDepthRenderer.renderProjectors(event.getModelViewMatrix(), event.getProjectionMatrix(), Minecraft.getInstance().getTimer());
        }
    }

    @SubscribeEvent
    public static void handleRenderGuiLayer(final RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.HOTBAR.equals(event.getName()) && !ClientPlatformImpl.isHotbarVisible()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleRenderNameTag(final RenderNameTagEvent event) {
        if (ProjectorDepthRenderer.shouldSuppressNameplates()) {
            event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void handleRenderFog(final ViewportEvent.RenderFog event) {
        ProjectorDepthRenderer.handleFog();
    }

    private ClientRenderEventsNeoForge() {
    }
}
