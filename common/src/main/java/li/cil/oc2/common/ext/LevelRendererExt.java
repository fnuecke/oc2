/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.ext;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

public interface LevelRendererExt {
    @Nullable
    Frustum oc2$getCullingFrustum();

    int oc2$getTicks();

    void oc2$cullTerrain(Camera camera, Frustum frustum);

    ChunkSectionsToRender oc2$prepareChunkRenders(Matrix4fc frustumMatrix, double cameraX, double cameraY, double cameraZ);
}
