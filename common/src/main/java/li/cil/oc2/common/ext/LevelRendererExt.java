/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.ext;

import net.minecraft.client.renderer.culling.Frustum;

public interface LevelRendererExt {
    Frustum getCullingFrustum();

    void setCullingFrustum(Frustum frustum);
}
