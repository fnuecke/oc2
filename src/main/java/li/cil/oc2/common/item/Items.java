package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.bus.device.data.Firmwares;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class Items {
    ///////////////////////////////////////////////////////////////////

    public static Item COMPUTER ;
    public static Item BUS_CABLE;
    public static BusInterfaceItem BUS_INTERFACE;
    public static Item NETWORK_CONNECTOR;
    public static Item NETWORK_HUB;
    public static Item REDSTONE_INTERFACE;
    public static Item DISK_DRIVE;
    public static Item CHARGER;
    public static Item CREATIVE_ENERGY;

    ///////////////////////////////////////////////////////////////////

    public static Item WRENCH;
    public static NetworkCableItem NETWORK_CABLE;
    public static Item ROBOT;

    public static MemoryItem MEMORY_SMALL;
    public static MemoryItem MEMORY_MEDIUM;
    public static MemoryItem MEMORY_LARGE;

    public static HardDriveItem HARD_DRIVE_SMALL;
    public static HardDriveItem HARD_DRIVE_MEDIUM;
    public static HardDriveItem HARD_DRIVE_LARGE;
    public static HardDriveWithExternalDataItem HARD_DRIVE_CUSTOM;
    public static FlashMemoryItem FLASH_MEMORY;
    public static FlashMemoryWithExternalDataItem FLASH_MEMORY_CUSTOM;
    public static FloppyItem FLOPPY;

    public static Item REDSTONE_INTERFACE_CARD;
    public static Item NETWORK_INTERFACE_CARD;
    public static Item FILE_IMPORT_EXPORT_CARD;

    public static Item INVENTORY_OPERATIONS_MODULE;
    public static Item BLOCK_OPERATIONS_MODULE;
    public static Item TRANSISTOR;
    public static Item CIRCUIT_BOARD;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ///////////////////////////////////////////////////////////////////

        COMPUTER = register(Blocks.COMPUTER);
        BUS_CABLE = register(Blocks.BUS_CABLE, new BusCableItem(Blocks.BUS_CABLE));
        BUS_INTERFACE = register("bus_interface", new BusInterfaceItem());
        NETWORK_CONNECTOR = register(Blocks.NETWORK_CONNECTOR);
        NETWORK_HUB = register(Blocks.NETWORK_HUB);
        REDSTONE_INTERFACE = register(Blocks.REDSTONE_INTERFACE);
        DISK_DRIVE = register(Blocks.DISK_DRIVE);
        CHARGER = register(Blocks.CHARGER, new ChargerItem(Blocks.CHARGER));
        CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY);

        ///////////////////////////////////////////////////////////////////

        WRENCH = register("wrench", new WrenchItem());

        NETWORK_CABLE = register("network_cable", new NetworkCableItem());
        ROBOT = register("robot", new RobotItem());

        MEMORY_SMALL = register("memory_small", new MemoryItem(2 * Constants.MEGABYTE));
        MEMORY_MEDIUM = register("memory_medium", new MemoryItem(4 * Constants.MEGABYTE));
        MEMORY_LARGE = register("memory_large", new MemoryItem(8 * Constants.MEGABYTE));

        HARD_DRIVE_SMALL = register("hard_drive_small", new HardDriveItem(2 * Constants.MEGABYTE, DyeColor.LIGHT_GRAY));
        HARD_DRIVE_MEDIUM = register("hard_drive_medium", new HardDriveItem(4 * Constants.MEGABYTE, DyeColor.GREEN));
        HARD_DRIVE_LARGE = register("hard_drive_large", new HardDriveItem(8 * Constants.MEGABYTE, DyeColor.CYAN));
        HARD_DRIVE_CUSTOM = register("hard_drive_custom", () -> new HardDriveWithExternalDataItem(BlockDeviceDataRegistration.BUILDROOT.getId(), DyeColor.BROWN));

        FLASH_MEMORY = register("flash_memory", new FlashMemoryItem(4 * Constants.KILOBYTE));
        FLASH_MEMORY_CUSTOM = register("flash_memory_custom", new FlashMemoryWithExternalDataItem(Firmwares.BUILDROOT.getId()));

        FLOPPY = register("floppy", new FloppyItem(512 * Constants.KILOBYTE));

        REDSTONE_INTERFACE_CARD = register("redstone_interface_card");
        NETWORK_INTERFACE_CARD = register("network_interface_card");
        FILE_IMPORT_EXPORT_CARD = register("file_import_export_card");

        INVENTORY_OPERATIONS_MODULE = register("inventory_operations_module");
        BLOCK_OPERATIONS_MODULE = register("block_operations_module", new BlockOperationsModule());
        TRANSISTOR = register("transistor", new ModItem());
        CIRCUIT_BOARD = register("circuit_board", new ModItem());
    }

    ///////////////////////////////////////////////////////////////////

    private static Item register(final String name) {
        return register(name, new ModItem());
    }

    private static <T extends Item> T register(final String name, final T factory) {
        return Registry.register(Registry.ITEM, new ResourceLocation(API.MOD_ID, name), factory);
    }

    private static <T extends Block> Item register(final T block) {
        return register(block, new ModBlockItem(block));
    }

    private static <TBlock extends Block, TItem extends Item> TItem register(final TBlock block, TItem item) {
        return register(Registry.BLOCK.getKey(block).getPath(), item);
    }
}
