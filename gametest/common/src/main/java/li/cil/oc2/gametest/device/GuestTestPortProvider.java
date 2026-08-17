/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.device;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;

import java.util.Optional;

public final class GuestTestPortProvider extends AbstractItemDeviceProvider {
    public GuestTestPortProvider() {
        super(GuestTestDevices.GUEST_TEST_PORT);
    }

    // ------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return Optional.of(new GuestTestPortDevice(query.getItemStack()));
    }
}
