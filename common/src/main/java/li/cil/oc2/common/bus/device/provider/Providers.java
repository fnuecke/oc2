/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider;

import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.block.*;
import li.cil.oc2.common.bus.device.provider.block.*;
import li.cil.oc2.common.bus.device.provider.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class Providers {
    public static Registrar<BlockDeviceProvider> blockDeviceProviderRegistry() {
        return ProviderRegistry.BLOCK_DEVICE_PROVIDER_REGISTRY;
    }

    public static Registrar<ItemDeviceProvider> itemDeviceProviderRegistry() {
        return ProviderRegistry.ITEM_DEVICE_PROVIDER_REGISTRY;
    }

    public static Optional<String> optionalKey(@Nullable final BlockDeviceProvider provider) {
        return optionalKey(blockDeviceProviderRegistry(), provider);
    }

    @SuppressWarnings("overloads")
    public static Optional<String> optionalKey(@Nullable final ItemDeviceProvider provider) {
        return optionalKey(itemDeviceProviderRegistry(), provider);
    }

    // --------------------------------------------------------------------- //

    private static <T> Optional<String> optionalKey(final Registrar<T> registrar, @Nullable final T value) {
        if (value == null) {
            return Optional.empty();
        }

        final ResourceLocation id = registrar.getId(value);
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }

    // --------------------------------------------------------------------- //

    public static void registerBlockDeviceProviders(final BiConsumer<String, Supplier<BlockDeviceProvider>> registry) {
        registry.accept("block", BlockStateObjectDeviceProvider::new);
        registry.accept("block_entity", BlockEntityObjectDeviceProvider::new);
        registry.accept("block_entity/capability", BlockEntityCapabilityDeviceProvider::new);

        registry.accept("energy_storage", EnergyStorageBlockDeviceProvider::new);
        registry.accept("fluid_handler", FluidHandlerBlockDeviceProvider::new);
        registry.accept("item_handler", ItemHandlerBlockDeviceProvider::new);

        registry.accept("disk_drive", DiskDriveDeviceProvider::new);
        registry.accept("flash_drive", FlashDriveDeviceProvider::new);

        registry.accept("beacon", () -> VanillaBlockDeviceProvider.of(BeaconBlockEntity.class, BeaconDevice::new));
        registry.accept("beehive", () -> VanillaBlockDeviceProvider.of(BeehiveBlockEntity.class, BeehiveDevice::new));
        registry.accept("brewing_stand", () -> VanillaBlockDeviceProvider.of(BrewingStandBlockEntity.class, BrewingStandDevice::new));
        registry.accept("comparator", () -> VanillaBlockDeviceProvider.of(Blocks.COMPARATOR, ComparatorDevice::new));
        registry.accept("composter", () -> VanillaBlockDeviceProvider.of(Blocks.COMPOSTER, ComposterDevice::new));
        registry.accept("crafter", () -> VanillaBlockDeviceProvider.of(CrafterBlockEntity.class, CrafterDevice::new));
        registry.accept("furnace", () -> VanillaBlockDeviceProvider.of(AbstractFurnaceBlockEntity.class, FurnaceDevice::new));
        registry.accept("jukebox", () -> VanillaBlockDeviceProvider.of(JukeboxBlockEntity.class, JukeboxDevice::new));
        registry.accept("lectern", () -> VanillaBlockDeviceProvider.of(LecternBlockEntity.class, LecternDevice::new));
        registry.accept("note_block", () -> VanillaBlockDeviceProvider.of(Blocks.NOTE_BLOCK, NoteBlockDevice::new));
        registry.accept("sign", () -> VanillaBlockDeviceProvider.of(SignBlockEntity.class, SignDevice::new));
        registry.accept("spawner", () -> VanillaBlockDeviceProvider.of(SpawnerBlockEntity.class, SpawnerDevice::new));
    }

    public static void registerItemDeviceProviders(final BiConsumer<String, Supplier<ItemDeviceProvider>> registry) {
        registry.accept("item_stack/capability", ItemStackCapabilityDeviceProvider::new);

        registry.accept("energy_storage", EnergyStorageItemDeviceProvider::new);
        registry.accept("fluid_handler", FluidHandlerItemDeviceProvider::new);
        registry.accept("item_handler", ItemHandlerItemDeviceProvider::new);

        registry.accept("file_import_export_card", FileImportExportCardItemDeviceProvider::new);
        registry.accept("flash_memory", FlashMemoryItemDeviceProvider::new);
        registry.accept("floppy", FloppyItemDeviceProvider::new);
        registry.accept("hard_drive", HardDriveItemDeviceProvider::new);
        registry.accept("memory", MemoryItemDeviceProvider::new);
        registry.accept("network_interface_card", NetworkInterfaceCardItemDeviceProvider::new);
        registry.accept("network_tunnel_card", NetworkTunnelCardItemDeviceProvider::new);
        registry.accept("redstone_interface_card", RedstoneInterfaceCardItemDeviceProvider::new);
        registry.accept("serial_interface_card", SerialInterfaceCardItemDeviceProvider::new);
        registry.accept("sound_card", SoundCardItemDeviceProvider::new);

        registry.accept("block_operations_module", BlockOperationsModuleDeviceProvider::new);
        registry.accept("inventory_operations_module", InventoryOperationsModuleDeviceProvider::new);
        registry.accept("network_tunnel_module", NetworkTunnelModuleItemDeviceProvider::new);
        registry.accept("scanner_module", ScannerModuleDeviceProvider::new);
        registry.accept("tank_operations_module", TankOperationsModuleDeviceProvider::new);
    }
}
