package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.API;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.IForgeRegistryEntry;

public interface DeviceType extends IForgeRegistryEntry<DeviceType> {
    ResourceLocation REGISTRY_ID = new ResourceLocation(API.MOD_ID, "device_types");

    ResourceLocation getIcon();

    ITextComponent getName();
}
