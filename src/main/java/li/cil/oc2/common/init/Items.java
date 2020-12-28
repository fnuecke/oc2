package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.item.*;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public final class Items {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Item> COMPUTER_ITEM = register(Constants.COMPUTER_BLOCK_NAME, Blocks.COMPUTER_BLOCK);
    public static final RegistryObject<Item> BUS_CABLE_ITEM = register(Constants.BUS_CABLE_BLOCK_NAME, Blocks.BUS_CABLE_BLOCK);
    public static final RegistryObject<Item> REDSTONE_INTERFACE_ITEM = register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, Blocks.REDSTONE_INTERFACE_BLOCK);
    public static final RegistryObject<Item> SCREEN_ITEM = register(Constants.SCREEN_BLOCK_NAME, Blocks.SCREEN_BLOCK);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Item> BUS_INTERFACE_ITEM = register(Constants.BUS_INTERFACE_ITEM_NAME, BusInterfaceItem::new);
    public static final RegistryObject<Item> WRENCH_ITEM = register(Constants.WRENCH_ITEM_NAME, WrenchItem::new);
    public static final RegistryObject<Item> HARD_DRIVE_ITEM = register(Constants.HARD_DRIVE_ITEM_NAME, HardDriveItem::new, new Item.Properties());
    public static final RegistryObject<Item> MEMORY_ITEM = register(Constants.MEMORY_ITEM_NAME, MemoryItem::new, new Item.Properties());

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    ///////////////////////////////////////////////////////////////////

    private static RegistryObject<Item> register(final String name) {
        return register(name, Item::new);
    }

    private static RegistryObject<Item> register(final String name, final Item.Properties properties) {
        return register(name, Item::new, properties);
    }

    private static <T extends Item> RegistryObject<T> register(final String name, final Function<Item.Properties, T> factory) {
        return register(name, factory, commonProperties());
    }

    private static <T extends Item> RegistryObject<T> register(final String name, final Function<Item.Properties, T> factory, final Item.Properties properties) {
        return ITEMS.register(name, () -> factory.apply(properties));
    }

    private static <T extends Block> RegistryObject<Item> register(final String name, final RegistryObject<T> block) {
        return register(name, (properties) -> new BlockItem(block.get(), properties));
    }

    private static Item.Properties commonProperties() {
        return new Item.Properties().group(ItemGroup.COMMON);
    }
}
