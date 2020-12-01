package li.cil.oc2;

import li.cil.ceres.Ceres;
import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.CommonSetup;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.block.RedstoneInterfaceBlock;
import li.cil.oc2.common.block.ScreenBlock;
import li.cil.oc2.common.container.ComputerContainer;
import li.cil.oc2.common.tile.BusCableTileEntity;
import li.cil.oc2.common.tile.ComputerTileEntity;
import li.cil.oc2.common.tile.RedstoneInterfaceTileEntity;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(API.MOD_ID)
public final class OpenComputers {
    public static final ItemGroup ITEM_GROUP = new ItemGroup(API.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(COMPUTER_ITEM.get());
        }
    };

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, API.MOD_ID);
    public static final RegistryObject<Block> COMPUTER_BLOCK = BLOCKS.register(Constants.COMPUTER_BLOCK_NAME, ComputerBlock::new);
    public static final RegistryObject<Block> BUS_CABLE_BLOCK = BLOCKS.register(Constants.BUS_CABLE_BLOCK_NAME, BusCableBlock::new);
    public static final RegistryObject<Block> REDSTONE_INTERFACE_BLOCK = BLOCKS.register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, RedstoneInterfaceBlock::new);
    public static final RegistryObject<Block> SCREEN_BLOCK = BLOCKS.register(Constants.SCREEN_BLOCK_NAME, ScreenBlock::new);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, API.MOD_ID);
    public static final RegistryObject<Item> COMPUTER_ITEM = ITEMS.register(Constants.COMPUTER_BLOCK_NAME, () -> new BlockItem(COMPUTER_BLOCK.get(), new Item.Properties().group(ITEM_GROUP)));
    public static final RegistryObject<Item> BUS_CABLE_ITEM = ITEMS.register(Constants.BUS_CABLE_BLOCK_NAME, () -> new BlockItem(BUS_CABLE_BLOCK.get(), new Item.Properties().group(ITEM_GROUP)));
    public static final RegistryObject<Item> REDSTONE_INTERFACE_ITEM = ITEMS.register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, () -> new BlockItem(REDSTONE_INTERFACE_BLOCK.get(), new Item.Properties().group(ITEM_GROUP)));
    public static final RegistryObject<Item> SCREEN_ITEM = ITEMS.register(Constants.SCREEN_BLOCK_NAME, () -> new BlockItem(SCREEN_BLOCK.get(), new Item.Properties().group(ITEM_GROUP)));

    public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, API.MOD_ID);
    public static final RegistryObject<TileEntityType<ComputerTileEntity>> COMPUTER_TILE_ENTITY = TILES.register(Constants.COMPUTER_BLOCK_NAME, () -> TileEntityType.Builder.create(ComputerTileEntity::new, COMPUTER_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<BusCableTileEntity>> BUS_CABLE_TILE_ENTITY = TILES.register(Constants.BUS_CABLE_BLOCK_NAME, () -> TileEntityType.Builder.create(BusCableTileEntity::new, BUS_CABLE_BLOCK.get()).build(null));
    public static final RegistryObject<TileEntityType<RedstoneInterfaceTileEntity>> REDSTONE_INTERFACE_TILE_ENTITY = TILES.register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, () -> TileEntityType.Builder.create(RedstoneInterfaceTileEntity::new, REDSTONE_INTERFACE_BLOCK.get()).build(null));

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, API.MOD_ID);
    public static final RegistryObject<ContainerType<ComputerContainer>> COMPUTER_CONTAINER = CONTAINERS.register(Constants.COMPUTER_BLOCK_NAME, () -> IForgeContainerType.create((id, inventory, data) -> {
        final BlockPos pos = data.readBlockPos();
        final TileEntity tileEntity = inventory.player.getEntityWorld().getTileEntity(pos);
        if (!(tileEntity instanceof ComputerTileEntity)) {
            return null;
        }
        return new ComputerContainer(id, (ComputerTileEntity) tileEntity);
    }));

    public OpenComputers() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(CommonSetup::run);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::run);

        // Do class lookup in a separate thread to avoid blocking for too long.
        // Specifically, this is to run detection of annotated types via the Reflections
        // library in the serialization library and the device tree registry.
        new Thread(() -> {
            Ceres.initialize();
            DeviceTreeRegistry.initialize();
        }).start();
    }
}
