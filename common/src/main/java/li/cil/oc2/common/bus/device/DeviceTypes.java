/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.DeviceTypeImpl;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Consumer;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class DeviceTypes {
    public static final Registrar<DeviceType> DEVICE_TYPE_REGISTRY = RegistryUtils.builder(DeviceType.REGISTRY).build();
    private static final DeferredRegister<DeviceType> DEVICE_TYPES = RegistryUtils.getInitializerFor(DeviceType.REGISTRY);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        register(ItemTags.DEVICES_MEMORY, t -> li.cil.oc2.api.bus.device.DeviceTypes.MEMORY = t);
        register(ItemTags.DEVICES_HARD_DRIVE, t -> li.cil.oc2.api.bus.device.DeviceTypes.HARD_DRIVE = t);
        register(ItemTags.DEVICES_FLASH_MEMORY, t -> li.cil.oc2.api.bus.device.DeviceTypes.FLASH_MEMORY = t);
        register(ItemTags.DEVICES_CARD, t -> li.cil.oc2.api.bus.device.DeviceTypes.CARD = t);
        register(ItemTags.DEVICES_ROBOT_MODULE, t -> li.cil.oc2.api.bus.device.DeviceTypes.ROBOT_MODULE = t);
        register(ItemTags.DEVICES_FLOPPY, t -> li.cil.oc2.api.bus.device.DeviceTypes.FLOPPY = t);
        register(ItemTags.DEVICES_NETWORK_TUNNEL, t -> li.cil.oc2.api.bus.device.DeviceTypes.NETWORK_TUNNEL = t);
    }

    public static String key(final DeviceType deviceType) {
        return Objects.requireNonNull(DEVICE_TYPE_REGISTRY.getId(deviceType)).toString();
    }

    ///////////////////////////////////////////////////////////////////

    private static void register(final TagKey<Item> tag, final Consumer<DeviceType> setter) {
        final String id = tag.location().getPath().replaceFirst("^devices/", "");
        DEVICE_TYPES.register(id, () -> (DeviceType) new DeviceTypeImpl(
            tag,
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "gui/icon/" + id),
            text("gui.{mod}.device_type." + id)
        )).listen(setter);
    }
}
