/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.NoteBlockDevice;
import li.cil.oc2.common.util.BlockLocation;
import net.minecraft.world.level.block.Blocks;

import java.lang.ref.WeakReference;

public final class NoteBlockDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        if (!query.getLevel().getBlockState(query.getQueryPosition()).is(Blocks.NOTE_BLOCK)) {
            return Invalidatable.empty();
        }

        final BlockLocation location = new BlockLocation(new WeakReference<>(query.getLevel()), query.getQueryPosition());
        return Invalidatable.of(new ObjectDevice(new NoteBlockDevice(location)));
    }
}
