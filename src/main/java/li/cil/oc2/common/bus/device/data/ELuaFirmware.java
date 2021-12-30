//package li.cil.oc2.common.bus.device.data;
//
//import li.cil.oc2.api.bus.device.data.Firmware;
//import li.cil.sedna.api.memory.MemoryMap;
//import li.cil.sedna.memory.MemoryMaps;
//import li.cil.sednamc.Sedna;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.TextComponent;
//import net.minecraftforge.registries.ForgeRegistryEntry;
//
//import java.io.IOException;
//
//public final class ELuaFirmware extends ForgeRegistryEntry<Firmware> implements Firmware {
//    @Override
//    public boolean run(final MemoryMap memory, final long startAddress) {
//        try {
//            MemoryMaps.store(memory, startAddress, Sedna.getELua());
//            return true;
//        } catch (final IOException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public Component getDisplayName() {
//        return new TextComponent("eLua");
//    }
//}
