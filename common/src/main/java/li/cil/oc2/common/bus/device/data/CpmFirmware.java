/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.cpm.Cpm;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class CpmFirmware implements Firmware {
    @Override
    public boolean run(final MemoryMap memory, final long startAddress) {
        try (InputStream stream = Cpm.getBootRom()) {
            MemoryMaps.store(memory, startAddress, ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN));
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("CP/M 2.2");
    }
}
