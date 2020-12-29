package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.DeviceType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;

public final class DeviceTypeImpl extends ForgeRegistryEntry<DeviceType> implements DeviceType {
    private final ResourceLocation icon;
    private final ITextComponent name;

    public DeviceTypeImpl(final ResourceLocation icon, final ITextComponent name) {
        this.icon = icon;
        this.name = name;
    }

    @Override
    public ResourceLocation getBackgroundIcon() {
        return icon;
    }

    @Override
    public ITextComponent getName() {
        return name;
    }
}
