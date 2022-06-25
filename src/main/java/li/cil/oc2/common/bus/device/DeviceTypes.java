/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.bus.device.util.DeviceTypeImpl;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class DeviceTypes {
    private static final DeferredRegister<DeviceType> DEVICE_TYPES = RegistryUtils.getInitializerFor(DeviceType.REGISTRY);

    ///////////////////////////////////////////////////////////////////

    public static final Supplier<IForgeRegistry<DeviceType>> DEVICE_TYPE_REGISTRY = DEVICE_TYPES.makeRegistry(DeviceType.class, RegistryBuilder::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        register(ItemTags.DEVICES_MEMORY);
        register(ItemTags.DEVICES_HARD_DRIVE);
        register(ItemTags.DEVICES_FLASH_MEMORY);
        register(ItemTags.DEVICES_CARD);
        register(ItemTags.DEVICES_ROBOT_MODULE);
        register(ItemTags.DEVICES_FLOPPY);
        register(ItemTags.DEVICES_NETWORK_TUNNEL);
    }

    ///////////////////////////////////////////////////////////////////

    private static void register(final TagKey<Item> tag) {
        final String id = tag.location().getPath().replaceFirst("^devices/", "");
        DEVICE_TYPES.register(id, () -> new DeviceTypeImpl(
            tag,
            new ResourceLocation(API.MOD_ID, "gui/icon/" + id),
            text("gui.{mod}.device_type." + id)
        ));
    }
}
