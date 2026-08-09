/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.Device;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractDeviceInfo<TProvider, TDevice extends Device> {
    @Nullable public final TProvider provider;
    public final TDevice device;

    ///////////////////////////////////////////////////////////////////

    protected AbstractDeviceInfo(@Nullable final TProvider provider, final TDevice device) {
        this.provider = provider;
        this.device = device;
    }

    ///////////////////////////////////////////////////////////////////

    public int getEnergyConsumption() {
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractDeviceInfo<?, ?> that = (AbstractDeviceInfo<?, ?>) o;
        return Objects.equals(provider, that.provider) && device.equals(that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, device);
    }
}
