/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

public final class ClientRenderEventsFabric {
    public static void initialize() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context ->
                NetworkCableRenderer.render(context.matrixStack(), context.camera(),
                        context.positionMatrix(), context.frustum()));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context ->
                BusInterfaceNameRenderer.INSTANCE.render(context.matrixStack()));

        WorldRenderEvents.LAST.register(context ->
                ProjectorDepthRenderer.renderProjectors(context.positionMatrix(), context.projectionMatrix(),
                        Minecraft.getInstance().getTimer()));
    }

    private ClientRenderEventsFabric() {
    }
}
