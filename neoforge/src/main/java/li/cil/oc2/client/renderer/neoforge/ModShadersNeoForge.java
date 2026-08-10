/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.neoforge;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.ModShaders;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

@EventBusSubscriber(modid = API.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModShadersNeoForge {
    @SubscribeEvent
    public static void handleRegisterShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
            event.getResourceProvider(),
            ModShaders.PROJECTORS_SHADER_LOCATION,
            DefaultVertexFormat.POSITION_TEX
        ), ModShaders::setProjectorsShader);
    }

    private ModShadersNeoForge() {
    }
}
