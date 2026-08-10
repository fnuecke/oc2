/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.neoforge;

import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class ProvidersImpl {
    public static void registerPlatformBlockDeviceProviders(final BiConsumer<String, Supplier<BlockDeviceProvider>> registry) {
        registry.accept("fluid_handler", FluidHandlerBlockDeviceProvider::new);
    }

    public static void registerPlatformItemDeviceProviders(final BiConsumer<String, Supplier<ItemDeviceProvider>> registry) {
        registry.accept("fluid_handler", FluidHandlerItemDeviceProvider::new);
    }

    private ProvidersImpl() {
    }
}
