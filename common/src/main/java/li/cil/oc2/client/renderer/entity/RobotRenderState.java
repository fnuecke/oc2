/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class RobotRenderState extends EntityRenderState {
    public float rotation;
    public float baseY;
    public float topY;
    public float topRotationY;
    public boolean isRunning;
    public int statusColor;
    public float statusValue;
    public long gameTime;
    public float partialTicks;
}
