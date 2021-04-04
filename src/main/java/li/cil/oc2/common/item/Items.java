package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Items {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Item> COMPUTER = register(Constants.COMPUTER_BLOCK_NAME, Blocks.COMPUTER);
    public static final RegistryObject<Item> BUS_CABLE = register(Constants.BUS_CABLE_BLOCK_NAME, Blocks.BUS_CABLE, BusCableItem::new);
    public static final RegistryObject<BusInterfaceItem> BUS_INTERFACE = register(Constants.BUS_INTERFACE_ITEM_NAME, Blocks.BUS_CABLE, BusInterfaceItem::new);
    public static final RegistryObject<Item> NETWORK_CONNECTOR = register(Constants.NETWORK_CONNECTOR_BLOCK_NAME, Blocks.NETWORK_CONNECTOR);
    public static final RegistryObject<Item> NETWORK_HUB = register(Constants.NETWORK_HUB_BLOCK_NAME, Blocks.NETWORK_HUB);
    public static final RegistryObject<Item> REDSTONE_INTERFACE = register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, Blocks.REDSTONE_INTERFACE);
    public static final RegistryObject<Item> DISK_DRIVE = register(Constants.DISK_DRIVE_BLOCK_NAME, Blocks.DISK_DRIVE);
    public static final RegistryObject<Item> CHARGER = register(Constants.CHARGER_BLOCK_NAME, Blocks.CHARGER, ChargerItem::new);
    public static final RegistryObject<Item> CREATIVE_ENERGY = register(Constants.CREATIVE_ENERGY_BLOCK_NAME, Blocks.CREATIVE_ENERGY);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Item> WRENCH = register(Constants.WRENCH_ITEM_NAME, WrenchItem::new);

    public static final RegistryObject<NetworkCableItem> NETWORK_CABLE = register(Constants.NETWORK_CABLE_ITEM_NAME, NetworkCableItem::new);
    public static final RegistryObject<Item> ROBOT = register(Constants.ROBOT_ENTITY_NAME, RobotItem::new);

    public static final RegistryObject<MemoryItem> MEMORY = register(Constants.MEMORY_ITEM_NAME, MemoryItem::new);
    public static final RegistryObject<HardDriveItem> HARD_DRIVE = register(Constants.HARD_DRIVE_ITEM_NAME, HardDriveItem::new);
    public static final RegistryObject<FlashMemoryItem> FLASH_MEMORY = register(Constants.FLASH_MEMORY_ITEM_NAME, FlashMemoryItem::new);
    public static final RegistryObject<Item> REDSTONE_INTERFACE_CARD = register(Constants.REDSTONE_INTERFACE_CARD_ITEM_NAME);
    public static final RegistryObject<Item> NETWORK_INTERFACE_CARD = register(Constants.NETWORK_INTERFACE_CARD_ITEM_NAME);
    public static final RegistryObject<FloppyItem> FLOPPY = register(Constants.FLOPPY_ITEM_NAME, FloppyItem::new);

    public static final RegistryObject<Item> INVENTORY_OPERATIONS_MODULE = register(Constants.INVENTORY_OPERATIONS_MODULE_ITEM_NAME);
    public static final RegistryObject<Item> BLOCK_OPERATIONS_MODULE = register(Constants.BLOCK_OPERATIONS_MODULE_ITEM_NAME, BlockOperationsModule::new);

    public static final RegistryObject<Item> TRANSISTOR = register(Constants.TRANSISTOR_ITEM_NAME, ModItem::new);
    public static final RegistryObject<Item> CIRCUIT_BOARD = register(Constants.CIRCUIT_BOARD_ITEM_NAME, ModItem::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    private static RegistryObject<Item> register(final String name) {
        return register(name, ModItem::new);
    }

    private static <T extends Item> RegistryObject<T> register(final String name, final Supplier<T> factory) {
        return ITEMS.register(name, factory);
    }

    private static <T extends Block> RegistryObject<Item> register(final String name, final RegistryObject<T> block) {
        return register(name, block, ModBlockItem::new);
    }

    private static <TBlock extends Block, TItem extends Item> RegistryObject<TItem> register(final String name, final RegistryObject<TBlock> block, final Function<TBlock, TItem> factory) {
        return register(name, () -> factory.apply(block.get()));
    }
}
