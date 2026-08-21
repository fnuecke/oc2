/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.fabric;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import li.cil.oc2.client.renderer.ModShaders;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;

public final class ModShadersFabric {
    public static void initialize() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(ModShaders.PROJECTORS_SHADER_LOCATION, DefaultVertexFormat.POSITION_TEX,
                ModShaders::setProjectorsShader);
            context.register(ModShaders.TERMINAL_SHADER_LOCATION, DefaultVertexFormat.POSITION_TEX_COLOR,
                ModShaders::setTerminalShader);
        });
    }

    private ModShadersFabric() {
    }
}
