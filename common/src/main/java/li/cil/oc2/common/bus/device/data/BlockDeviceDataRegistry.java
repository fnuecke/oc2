/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.util.RegistryUtils;
import li.cil.oc2.common.vm.CpmSystemDisk;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.cpm.Cpm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import static li.cil.oc2.common.util.TextFormatUtils.formatSize;

public final class BlockDeviceDataRegistry {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DIRECTORY = "block_devices";
    private static final String HARD_DRIVE_MEDIUM = "hdd";
    private static final String FLOPPY_MEDIUM = "floppy";
    private static final String FLASH_MEDIUM = "flash";
    private static final String IMAGE_EXTENSION = ".bin";
    private static final String DESCRIPTOR_EXTENSION = ".json";

    private static final Registrar<BlockDeviceData> REGISTRY = RegistryUtils.builder(Registries.BLOCK_DEVICE_DATA).build();
    private static final DeferredRegister<BlockDeviceData> INITIALIZER = RegistryUtils.getInitializerFor(Registries.BLOCK_DEVICE_DATA);

    private static final Map<ResourceLocation, BlockDeviceData> DATAPACK_DATA = new HashMap<>();

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<BlockDeviceData> BUILDROOT =
        INITIALIZER.register(path(HARD_DRIVE_MEDIUM, "sedna"), () -> new BuiltinBlockDeviceData(
            BuiltinBlockDeviceData.readOnce(Buildroot::getRootFilesystem), "Sedna Linux", DyeColor.GREEN));
    public static final RegistrySupplier<BlockDeviceData> CPM =
        INITIALIZER.register(path(FLOPPY_MEDIUM, "cpm"), () -> new BuiltinBlockDeviceData(
            BuiltinBlockDeviceData.readEachTime(CpmSystemDisk::getImage), "CP/M 2.2", DyeColor.ORANGE));

    public static final RegistrySupplier<BlockDeviceData> FIRMWARE_RISCV =
        INITIALIZER.register(path(FLASH_MEDIUM, "riscv"), () -> new BuiltinBlockDeviceData(
            BuiltinBlockDeviceData.readOnce(Buildroot::getSednaFirmware), "Sedna Linux", DyeColor.GREEN));
    public static final RegistrySupplier<BlockDeviceData> FIRMWARE_Z80 =
        INITIALIZER.register(path(FLASH_MEDIUM, "z80"), () -> new BuiltinBlockDeviceData(
            BuiltinBlockDeviceData.readOnce(Cpm::getBootRom), "CP/M 2.2", DyeColor.ORANGE));

    // --------------------------------------------------------------------- //

    public static void initialize() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ReloadListener.INSTANCE,
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, DIRECTORY));
        LifecycleEvent.SERVER_STOPPED.register(server -> reset());
    }

    @Nullable
    public static ResourceLocation getKey(final BlockDeviceData data) {
        if (data instanceof final DatapackBlockDeviceData datapackData) {
            return datapackData.getLocation();
        }

        return REGISTRY.getId(data);
    }

    @Nullable
    public static BlockDeviceData getValue(final ResourceLocation location) {
        final BlockDeviceData data = DATAPACK_DATA.get(location);
        return data != null ? data : REGISTRY.get(location);
    }

    public static Stream<BlockDeviceData> hardDriveValues() {
        return values(HARD_DRIVE_MEDIUM);
    }

    public static Stream<BlockDeviceData> floppyValues() {
        return values(FLOPPY_MEDIUM);
    }

    public static Stream<BlockDeviceData> firmwareValues() {
        return values(FLASH_MEDIUM);
    }

    // --------------------------------------------------------------------- //

    private static String path(final String medium, final String name) {
        return DIRECTORY + "/" + medium + "/" + name + IMAGE_EXTENSION;
    }

    private static String getMedium(final ResourceLocation location) {
        final String path = location.getPath();
        if (!path.startsWith(DIRECTORY + "/")) {
            return "";
        }

        final String relative = path.substring(DIRECTORY.length() + 1);
        final int separator = relative.indexOf('/');
        return separator < 0 ? "" : relative.substring(0, separator);
    }

    private static Stream<BlockDeviceData> values(final String medium) {
        final Map<ResourceLocation, BlockDeviceData> values = new LinkedHashMap<>();
        for (final BlockDeviceData data : REGISTRY) {
            final ResourceLocation id = REGISTRY.getId(data);
            if (id != null && medium.equals(getMedium(id))) {
                values.put(id, data);
            }
        }
        DATAPACK_DATA.forEach((location, data) -> {
            if (medium.equals(getMedium(location))) {
                values.put(location, data);
            }
        });
        return values.values().stream();
    }

    private static void reset() {
        for (final BlockDeviceData data : DATAPACK_DATA.values()) {
            try {
                ((DatapackBlockDeviceData) data).close();
            } catch (final Exception e) {
                LOGGER.error(e);
            }
        }
        DATAPACK_DATA.clear();
    }

    private static void reload(final ResourceManager resourceManager) {
        reset();

        LOGGER.info("Searching for datapack block devices...");
        final Map<ResourceLocation, Resource> descriptors = resourceManager
            .listResources(DIRECTORY, location -> location.getPath().endsWith(DESCRIPTOR_EXTENSION));

        for (final Map.Entry<ResourceLocation, Resource> entry : descriptors.entrySet()) {
            final ResourceLocation descriptorLocation = entry.getKey();
            LOGGER.info("Found [{}]", descriptorLocation);
            try {
                final String medium = getMedium(descriptorLocation);
                if (!HARD_DRIVE_MEDIUM.equals(medium) && !FLOPPY_MEDIUM.equals(medium) && !FLASH_MEDIUM.equals(medium)) {
                    LOGGER.error("  Not in one of the [{}], [{}] or [{}] directories.",
                        HARD_DRIVE_MEDIUM, FLOPPY_MEDIUM, FLASH_MEDIUM);
                    continue;
                }

                final JsonObject json;
                try (Reader reader = entry.getValue().openAsReader()) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }

                final String name = json.has("name") ? json.getAsJsonPrimitive("name").getAsString() : "???";
                final DyeColor color = json.has("color")
                    ? DyeColor.byName(json.getAsJsonPrimitive("color").getAsString(), null)
                    : null;

                final ResourceLocation location = getImageLocation(descriptorLocation);
                final DatapackBlockDeviceData data = new DatapackBlockDeviceData(resourceManager, location, name, color);

                LOGGER.info("  Adding [{}] with id [{}] and a size of [{}].",
                    name, location, formatSize(data.getBlockDevice().getCapacity()));
                DATAPACK_DATA.put(location, data);
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        }
    }

    private static ResourceLocation getImageLocation(final ResourceLocation descriptorLocation) {
        final String path = descriptorLocation.getPath();
        return descriptorLocation.withPath(path.substring(0, path.length() - DESCRIPTOR_EXTENSION.length()) + IMAGE_EXTENSION);
    }

    // --------------------------------------------------------------------- //

    private static final class ReloadListener implements PreparableReloadListener {
        public static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public CompletableFuture<Void> reload(final PreparableReloadListener.PreparationBarrier stage, final ResourceManager resourceManager, final ProfilerFiller preparationsProfiler, final ProfilerFiller reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
            return CompletableFuture
                .runAsync(() -> BlockDeviceDataRegistry.reload(resourceManager), backgroundExecutor)
                .thenCompose(stage::wait);
        }
    }

    private BlockDeviceDataRegistry() {
    }
}
