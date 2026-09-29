/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.storage;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.sedna.api.device.BlockDevice;

public abstract class AbstractBlockStorageItemDevice<TBlock extends BlockDevice, TIdentity> extends AbstractBlockStorageDevice<TBlock, TIdentity> implements ItemDevice {
    protected AbstractBlockStorageItemDevice(final TIdentity identity, final boolean readonly) {
        super(identity, readonly);
    }
}
