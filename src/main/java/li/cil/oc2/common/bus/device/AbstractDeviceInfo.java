package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import net.minecraft.util.registry.Registry;

import java.util.Objects;

public abstract class AbstractDeviceInfo<TProvider, TDevice extends Device> {
    public final Registry<TProvider> registry;
    public final TProvider provider;
    public final TDevice device;

    ///////////////////////////////////////////////////////////////////

    protected AbstractDeviceInfo(final Registry<TProvider> registry, final TProvider provider, final TDevice device) {
        this.registry = registry;
        this.provider = provider;
        this.device = device;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractDeviceInfo<?, ?> that = (AbstractDeviceInfo<?, ?>) o;
        return provider.equals(that.provider) && device.equals(that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, device);
    }
}
