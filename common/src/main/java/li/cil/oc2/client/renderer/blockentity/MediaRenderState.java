/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class MediaRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState media = new ItemStackRenderState();
    public int neighborLight;
}
