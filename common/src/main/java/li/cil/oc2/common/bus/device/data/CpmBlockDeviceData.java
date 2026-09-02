/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.vm.CpmSystemDisk;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.nio.ByteBuffer;

public final class CpmBlockDeviceData implements BlockDeviceData {
    @Override
    public BlockDevice getBlockDevice() {
        return ByteBufferBlockDevice.wrap(ByteBuffer.wrap(CpmSystemDisk.getImage()), true);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("CP/M 2.2");
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.ORANGE;
    }
}
