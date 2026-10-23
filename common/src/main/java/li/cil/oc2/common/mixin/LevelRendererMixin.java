/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import li.cil.oc2.client.renderer.ProjectorDepthRenderer;
import li.cil.oc2.common.ext.LevelRendererExt;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExt {
    @Shadow
    private int ticks;

    @Unique
    @Nullable
    private Frustum oc2$cullingFrustum;

    @Shadow
    protected abstract void cullTerrain(Camera camera, Frustum frustum, boolean spectator);

    @Shadow
    protected abstract ChunkSectionsToRender prepareChunkRenders(Matrix4fc frustumMatrix, double x, double y, double z);

    @Override
    @Nullable
    public Frustum oc2$getCullingFrustum() {
        return oc2$cullingFrustum;
    }

    @Override
    public int oc2$getTicks() {
        return ticks;
    }

    @Override
    public void oc2$cullTerrain(final Camera camera, final Frustum frustum) {
        cullTerrain(camera, frustum, false);
    }

    @Override
    public ChunkSectionsToRender oc2$prepareChunkRenders(final Matrix4fc frustumMatrix, final double cameraX, final double cameraY, final double cameraZ) {
        return prepareChunkRenders(frustumMatrix, cameraX, cameraY, cameraZ);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void captureMainCamera(final GraphicsResourceAllocator graphicsResourceAllocator, final DeltaTracker deltaTracker, final boolean renderBlockOutline, final Camera camera, final Matrix4f frustumMatrix, final Matrix4f projectionMatrix, final Matrix4f cullingProjectionMatrix, final GpuBufferSlice shaderFog, final Vector4f fogColor, final boolean renderSky, final CallbackInfo ci) {
        ProjectorDepthRenderer.captureMainCamera(frustumMatrix, projectionMatrix);
    }

    @ModifyArg(method = "renderLevel", index = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"))
    private Frustum captureCullingFrustum(final Frustum frustum) {
        oc2$cullingFrustum = frustum;
        return frustum;
    }
}
