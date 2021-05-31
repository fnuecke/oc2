package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.util.text.Component;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.io.IOException;

public final class BuildrootFirmware extends ForgeRegistryEntry<Firmware> implements Firmware {
    @Override
    public boolean run(final MemoryMap memory, final long startAddress) {
        try {
            MemoryMaps.store(memory, startAddress, Buildroot.getFirmware());
            MemoryMaps.store(memory, startAddress + 0x200000, Buildroot.getLinuxImage());
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    @Override
    public Component getDisplayName() {
        return new StringTextComponent("Linux");
    }
}
