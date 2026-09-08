/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.device;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import net.minecraft.world.item.Item;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;

public final class GuestTestDevices {
    private static final String NAME = "guest_test_port";

    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(MOD_ID, net.minecraft.core.registries.Registries.ITEM);
    private static final DeferredRegister<ItemDeviceProvider> PROVIDERS =
        DeferredRegister.create(MOD_ID, li.cil.oc2.api.util.Registries.ITEM_DEVICE_PROVIDER);

    public static final RegistrySupplier<Item> GUEST_TEST_PORT =
        ITEMS.register(NAME, () -> new Item(new Item.Properties()));

    // --------------------------------------------------------------------- //

    public static void initialize() {
        PROVIDERS.register(NAME, GuestTestPortProvider::new);

        ITEMS.register();
        PROVIDERS.register();
    }

    // --------------------------------------------------------------------- //

    private GuestTestDevices() {
    }
}
