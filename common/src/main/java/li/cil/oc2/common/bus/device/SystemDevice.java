/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

@IODeviceDescription(name = "SYSTEM", description = """
    Provided by every computer and robot, always as MLAPI-device index 0.

    Items, fluids and blocks are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. Ids are two bytes, low byte first.

    An unknown id or name fails with `OCEARG`.""")
public final class SystemDevice {
    private static final int GET_ITEM_NAME_CODE = 1;
    private static final int GET_ITEM_ID_CODE = 2;
    private static final int GET_FLUID_NAME_CODE = 3;
    private static final int GET_FLUID_ID_CODE = 4;
    private static final int GET_BLOCK_NAME_CODE = 5;
    private static final int GET_BLOCK_ID_CODE = 6;

    private static final String NAME_ARGUMENT = "the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.";
    private static final String NAME_RESULT_SUFFIX = " Read while `OCDAV` is set to read fully.";

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ITEM_NAME_CODE, synchronize = false,
        description = "Reads the name of an item.",
        argumentsDescription = "two bytes, the item id.",
        resultsDescription = "the name, such as `minecraft:redstone`." + NAME_RESULT_SUFFIX)
    public void getItemName(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        writeName(BuiltInRegistries.ITEM, arguments, results);
    }

    @IOCallback(value = GET_ITEM_ID_CODE, synchronize = false,
        description = "Looks an item up by name.",
        argumentsDescription = NAME_ARGUMENT,
        resultsDescription = "two bytes, the item id.")
    public void getItemId(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        writeId(BuiltInRegistries.ITEM, arguments, results);
    }

    @IOCallback(value = GET_BLOCK_NAME_CODE, synchronize = false,
        description = "Reads the name of a block.",
        argumentsDescription = "two bytes, the block id.",
        resultsDescription = "the name, such as `minecraft:stone`." + NAME_RESULT_SUFFIX)
    public void getBlockName(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        writeName(BuiltInRegistries.BLOCK, arguments, results);
    }

    @IOCallback(value = GET_BLOCK_ID_CODE, synchronize = false,
        description = "Looks a block up by name.",
        argumentsDescription = NAME_ARGUMENT,
        resultsDescription = "two bytes, the block id.")
    public void getBlockId(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        writeId(BuiltInRegistries.BLOCK, arguments, results);
    }

    @IOCallback(value = GET_FLUID_NAME_CODE, synchronize = false,
        description = "Reads the name of a fluid.",
        argumentsDescription = "two bytes, the fluid id.",
        resultsDescription = "the name, such as `minecraft:water`." + NAME_RESULT_SUFFIX)
    public void getFluidName(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        writeName(BuiltInRegistries.FLUID, arguments, results);
    }

    @IOCallback(value = GET_FLUID_ID_CODE, synchronize = false,
        description = "Looks a fluid up by name.",
        argumentsDescription = NAME_ARGUMENT,
        resultsDescription = "two bytes, the fluid id.")
    public void getFluidId(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        writeId(BuiltInRegistries.FLUID, arguments, results);
    }

    public static <T> int toId(final Registry<T> registry, final T value) throws IOException {
        final int id = registry.getId(value);
        if (id > 0xFFFF) {
            throw new IOException(kind(registry) + " id does not fit the guest protocol: " + id);
        }
        return id;
    }

    // --------------------------------------------------------------------- //

    private static void writeName(final Registry<?> registry, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int id = arguments.readU16();
        final ResourceLocation key = registry.getHolder(id)
            .orElseThrow(() -> new IllegalArgumentException("no " + kind(registry) + " with id: " + id))
            .key().location();
        results.writeString(key.toString());
    }

    private static <T> void writeId(final Registry<T> registry, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final String name = arguments.readString();
        final ResourceLocation key = ResourceLocation.tryParse(name);
        final T value = key != null ? registry.getOptional(key).orElse(null) : null;
        if (value == null) {
            throw new IllegalArgumentException("no such " + kind(registry) + ": " + name);
        }

        results.writeU16(toId(registry, value));
    }

    private static String kind(final Registry<?> registry) {
        return registry.key().location().getPath();
    }
}
