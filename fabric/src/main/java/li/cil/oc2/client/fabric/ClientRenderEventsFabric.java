/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import li.cil.oc2.client.renderer.BusInterfaceNameRenderer;
import li.cil.oc2.client.renderer.NetworkCableRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;

import javax.annotation.Nullable;

public final class ClientRenderEventsFabric {
    @Nullable
    private static Camera camera;
    @Nullable
    private static Frustum frustum;

    // --------------------------------------------------------------------- //

    public static void initialize() {
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            camera = context.camera();
            frustum = context.frustum();
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            if (camera != null && frustum != null) {
                NetworkCableRenderer.render(camera, context.matrices().last().pose(), frustum);
            }
        });

        WorldRenderEvents.END_MAIN.register(context ->
            BusInterfaceNameRenderer.INSTANCE.render(context.matrices()));
    }

    private ClientRenderEventsFabric() {
    }
}
