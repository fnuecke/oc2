/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.data.FirmwareRegistry;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Items {
    private static final DeferredRegister<Item> ITEMS = RegistryUtils.getInitializerFor(Registries.ITEM);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<Item> BUS_CABLE = register(Blocks.BUS_CABLE, BusCableItem::new);
    public static final RegistrySupplier<BusInterfaceItem> BUS_INTERFACE = register("bus_interface", BusInterfaceItem::new);
    public static final RegistrySupplier<Item> CHARGER = register(Blocks.CHARGER, ChargerItem::new);
    public static final RegistrySupplier<Item> COMPUTER = register(Blocks.COMPUTER, ComputerItem::new);
    public static final RegistrySupplier<Item> CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY);
    public static final RegistrySupplier<Item> DISK_DRIVE = register(Blocks.DISK_DRIVE);
    public static final RegistrySupplier<Item> KEYBOARD = register(Blocks.KEYBOARD);
    public static final RegistrySupplier<Item> NETWORK_CONNECTOR = register(Blocks.NETWORK_CONNECTOR);
    public static final RegistrySupplier<Item> NETWORK_HUB = register(Blocks.NETWORK_HUB);
    public static final RegistrySupplier<Item> PROJECTOR = register(Blocks.PROJECTOR);
    public static final RegistrySupplier<Item> REDSTONE_INTERFACE = register(Blocks.REDSTONE_INTERFACE);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<Item> WRENCH = register("wrench", WrenchItem::new);
    public static final RegistrySupplier<Item> MANUAL = register("manual", ManualItem::new);

    public static final RegistrySupplier<Item> ROBOT = register("robot", RobotItem::new);
    public static final RegistrySupplier<NetworkCableItem> NETWORK_CABLE = register("network_cable", NetworkCableItem::new);

    public static final RegistrySupplier<MemoryItem> MEMORY_SMALL = register("memory_small", () ->
        new MemoryItem(2 * Constants.MEGABYTE));
    public static final RegistrySupplier<MemoryItem> MEMORY_MEDIUM = register("memory_medium", () ->
        new MemoryItem(4 * Constants.MEGABYTE));
    public static final RegistrySupplier<MemoryItem> MEMORY_LARGE = register("memory_large", () ->
        new MemoryItem(8 * Constants.MEGABYTE));

    public static final RegistrySupplier<HardDriveItem> HARD_DRIVE_SMALL = register("hard_drive_small", () ->
        new HardDriveItem(2 * Constants.MEGABYTE, DyeColor.LIGHT_GRAY));
    public static final RegistrySupplier<HardDriveItem> HARD_DRIVE_MEDIUM = register("hard_drive_medium", () ->
        new HardDriveItem(4 * Constants.MEGABYTE, DyeColor.GREEN));
    public static final RegistrySupplier<HardDriveItem> HARD_DRIVE_LARGE = register("hard_drive_large", () ->
        new HardDriveItem(8 * Constants.MEGABYTE, DyeColor.CYAN));
    public static final RegistrySupplier<HardDriveWithExternalDataItem> HARD_DRIVE_CUSTOM = register("hard_drive_custom", () ->
        new HardDriveWithExternalDataItem(BlockDeviceDataRegistry.BUILDROOT.getId(), DyeColor.BROWN));

    public static final RegistrySupplier<FlashMemoryItem> FLASH_MEMORY = register("flash_memory", () ->
        new FlashMemoryItem(4 * Constants.KILOBYTE));
    public static final RegistrySupplier<FlashMemoryWithExternalDataItem> FLASH_MEMORY_CUSTOM = register("flash_memory_custom", () ->
        new FlashMemoryWithExternalDataItem(FirmwareRegistry.BUILDROOT.getId()));

    public static final RegistrySupplier<FloppyItem> FLOPPY = register("floppy", () ->
        new FloppyItem(512 * Constants.KILOBYTE));

    public static final RegistrySupplier<Item> REDSTONE_INTERFACE_CARD = register("redstone_interface_card");
    public static final RegistrySupplier<Item> NETWORK_INTERFACE_CARD = register("network_interface_card", NetworkInterfaceCardItem::new);
    public static final RegistrySupplier<Item> NETWORK_TUNNEL_CARD = register("network_tunnel_card", NetworkTunnelItem::new);
    public static final RegistrySupplier<Item> FILE_IMPORT_EXPORT_CARD = register("file_import_export_card");
    public static final RegistrySupplier<Item> SOUND_CARD = register("sound_card");

    public static final RegistrySupplier<Item> INVENTORY_OPERATIONS_MODULE = register("inventory_operations_module");
    public static final RegistrySupplier<Item> BLOCK_OPERATIONS_MODULE = register("block_operations_module", BlockOperationsModule::new);
    public static final RegistrySupplier<Item> NETWORK_TUNNEL_MODULE = register("network_tunnel_module", NetworkTunnelItem::new);

    public static final RegistrySupplier<Item> TRANSISTOR = register("transistor", ModItem::new);
    public static final RegistrySupplier<Item> CIRCUIT_BOARD = register("circuit_board", ModItem::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }

    ///////////////////////////////////////////////////////////////////

    private static RegistrySupplier<Item> register(final String name) {
        return register(name, ModItem::new);
    }

    private static <T extends Item> RegistrySupplier<T> register(final String name, final Supplier<T> factory) {
        return ITEMS.register(name, factory);
    }

    private static <T extends Block> RegistrySupplier<Item> register(final RegistrySupplier<T> block) {
        return register(block, ModBlockItem::new);
    }

    private static <TBlock extends Block, TItem extends Item> RegistrySupplier<TItem> register(final RegistrySupplier<TBlock> block, final Function<TBlock, TItem> factory) {
        return register(block.getId().getPath(), () -> factory.apply(block.get()));
    }
}
