package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.DeviceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistryEntry;

public final class DeviceTypeImpl extends ForgeRegistryEntry<DeviceType> implements DeviceType {
    private final ResourceLocation icon;
    private final Component name;

    public DeviceTypeImpl(final ResourceLocation icon, final Component name) {
        this.icon = icon;
        this.name = name;
    }

    @Override
    public ResourceLocation getBackgroundIcon() {
        return icon;
    }

    @Override
    public Component getName() {
        return name;
    }
}
