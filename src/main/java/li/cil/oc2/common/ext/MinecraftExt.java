/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.ext;

import com.mojang.blaze3d.pipeline.RenderTarget;

import javax.annotation.Nullable;

public interface MinecraftExt {
    void setMainRenderTargetOverride(@Nullable RenderTarget renderTarget);
}
