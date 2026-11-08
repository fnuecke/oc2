/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.UiScreen;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractBlockEntityScreen<T extends ModBlockEntity> extends UiScreen {
    private static final int USE_DISTANCE = 8;

    // --------------------------------------------------------------------- //

    protected final T blockEntity;

    // --------------------------------------------------------------------- //

    protected AbstractBlockEntityScreen(final Component title, final T blockEntity) {
        super(title, Sprites.THEME);
        this.blockEntity = blockEntity;
    }

    // --------------------------------------------------------------------- //

    public static boolean isStillValid(final ModBlockEntity blockEntity) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 blockCenter = Vec3.atCenterOf(blockEntity.getBlockPos());
        return blockEntity.isValid() &&
            minecraft.player != null &&
            minecraft.player.distanceToSqr(blockCenter) <= USE_DISTANCE * USE_DISTANCE;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void tick() {
        super.tick();

        if (isStillValid(blockEntity)) {
            safeTick();
        } else {
            onClose();
        }
    }

    // --------------------------------------------------------------------- //

    protected void safeTick() {
    }
}
