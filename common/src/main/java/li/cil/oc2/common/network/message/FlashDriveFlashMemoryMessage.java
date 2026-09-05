/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.AbstractRemovableMediaBlockEntity;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class FlashDriveFlashMemoryMessage extends AbstractRemovableMediaMessage {
    public FlashDriveFlashMemoryMessage(final FlashDriveBlockEntity flashDrive) {
        super(flashDrive);
    }

    public FlashDriveFlashMemoryMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Class<? extends AbstractRemovableMediaBlockEntity<?>> getBlockEntityType() {
        return FlashDriveBlockEntity.class;
    }
}
