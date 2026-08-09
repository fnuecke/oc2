/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.util;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * {@link ResourceKey}s for registries created by the mod.
 */
public final class Registries {
    /**
     * The registry name of the registry holding block device providers.
     */
    public static final ResourceKey<Registry<BlockDeviceProvider>> BLOCK_DEVICE_PROVIDER = key("block_device_provider");

    /**
     * The registry name of the registry holding item device providers.
     */
    public static final ResourceKey<Registry<ItemDeviceProvider>> ITEM_DEVICE_PROVIDER = key("item_device_provider");

    /**
     * The registry name of the registry holding block device bases.
     */
    public static final ResourceKey<Registry<BlockDeviceData>> BLOCK_DEVICE_DATA = key("block_device_data");

    /**
     * The registry name of the registry holding firmwares.
     */
    public static final ResourceKey<Registry<Firmware>> FIRMWARE = key("firmware");

    ///////////////////////////////////////////////////////////////////

    private static <T> ResourceKey<Registry<T>> key(final String name) {
        return ResourceKey.createRegistryKey(new ResourceLocation(API.MOD_ID, name));
    }
}
