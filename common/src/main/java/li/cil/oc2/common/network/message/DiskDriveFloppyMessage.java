/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.AbstractRemovableMediaBlockEntity;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class DiskDriveFloppyMessage extends AbstractRemovableMediaMessage {
    public DiskDriveFloppyMessage(final DiskDriveBlockEntity diskDrive) {
        super(diskDrive);
    }

    public DiskDriveFloppyMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Class<? extends AbstractRemovableMediaBlockEntity<?>> getBlockEntityType() {
        return DiskDriveBlockEntity.class;
    }
}
