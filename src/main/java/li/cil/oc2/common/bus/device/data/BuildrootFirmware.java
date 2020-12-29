package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.memory.MemoryMaps;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class BuildrootFirmware extends ForgeRegistryEntry<Firmware> implements Firmware {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void run(final MemoryMap memory, final long startAddress) {
        try {
            MemoryMaps.store(memory, startAddress, Buildroot.getFirmware());
            MemoryMaps.store(memory, startAddress + 0x200000, Buildroot.getLinuxImage());
        } catch (final IOException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public ITextComponent getName() {
        return new StringTextComponent("OpenSBI+Linux");
    }
}
