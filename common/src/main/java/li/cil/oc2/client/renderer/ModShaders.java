/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

public final class ModShaders {
    public static final int MAX_PROJECTORS = 3;

    public static final ResourceLocation PROJECTORS_SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "projectors");
    public static final ResourceLocation TERMINAL_SHADER_LOCATION = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "terminal");
    private static final String[] PROJECTOR_COLOR_NAMES = {"ProjectorColor0", "ProjectorColor1", "ProjectorColor2"};
    private static final String[] PROJECTOR_DEPTH_NAMES = {"ProjectorDepth0", "ProjectorDepth1", "ProjectorDepth2"};
    private static final String[] PROJECTOR_CAMERA_NAMES = {"ProjectorCamera0", "ProjectorCamera1", "ProjectorCamera2"};

    // --------------------------------------------------------------------- //

    private static ShaderInstance projectorsShader;
    private static ShaderInstance terminalShader;

    // --------------------------------------------------------------------- //

    @Nullable
    public static ShaderInstance getProjectorsShader() {
        return projectorsShader;
    }

    @Nullable
    public static ShaderInstance getTerminalShader() {
        return terminalShader;
    }

    @SuppressWarnings("ConstantConditions") // Setting samples to null to clear them is fine.
    public static void configureProjectorsShader(
        final RenderTarget target,
        final Matrix4f inverseCameraMatrix,
        final DynamicTexture[] colors,
        final RenderTarget[] depths,
        final Matrix4f[] projectorCameraMatrices,
        final int count
    ) {
        final int projectorCount = Math.min(count, MAX_PROJECTORS);
        projectorsShader.safeGetUniform("Count").set(projectorCount);

        projectorsShader.setSampler("MainCameraDepth", target.getDepthTextureId());
        projectorsShader.safeGetUniform("InverseMainCamera").set(inverseCameraMatrix);

        for (int i = 0; i < MAX_PROJECTORS; i++) {
            if (i < projectorCount) {
                projectorsShader.setSampler(PROJECTOR_COLOR_NAMES[i], colors[i].getId());
                projectorsShader.setSampler(PROJECTOR_DEPTH_NAMES[i], depths[i].getDepthTextureId());
                projectorsShader.safeGetUniform(PROJECTOR_CAMERA_NAMES[i]).set(projectorCameraMatrices[i]);
            } else {
                projectorsShader.setSampler(PROJECTOR_COLOR_NAMES[i], null);
                projectorsShader.setSampler(PROJECTOR_DEPTH_NAMES[i], null);
            }
        }
    }

    public static void setProjectorsShader(@Nullable final ShaderInstance shader) {
        projectorsShader = shader;
    }

    public static void setTerminalShader(@Nullable final ShaderInstance shader) {
        terminalShader = shader;
    }
}
