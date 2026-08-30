/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.bus.device.util.DeviceTypeImpl;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Objects;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class DeviceTypeRegistry {
    public static final Registrar<DeviceType> REGISTRY = RegistryUtils.builder(Registries.DEVICE_TYPE).build();
    private static final DeferredRegister<DeviceType> DEVICE_TYPES = RegistryUtils.getInitializerFor(Registries.DEVICE_TYPE);

    // --------------------------------------------------------------------- //

    public static void initialize() {
        register(ItemTags.DEVICES_CPU);
        register(ItemTags.DEVICES_MEMORY);
        register(ItemTags.DEVICES_HARD_DRIVE);
        register(ItemTags.DEVICES_FLASH_MEMORY);
        register(ItemTags.DEVICES_CARD);
        register(ItemTags.DEVICES_ROBOT_MODULE);
        register(ItemTags.DEVICES_FLOPPY);
        register(ItemTags.DEVICES_NETWORK_TUNNEL);
    }

    public static String key(final DeviceType deviceType) {
        return Objects.requireNonNull(REGISTRY.getId(deviceType)).toString();
    }

    // --------------------------------------------------------------------- //

    private static void register(final TagKey<Item> tag) {
        final String id = tag.location().getPath().replaceFirst("^devices/", "");
        DEVICE_TYPES.register(id, () -> (DeviceType) new DeviceTypeImpl(
            tag,
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "gui/icon/" + id),
            text("gui.{mod}.device_type." + id)
        ));
    }
}
