package li.cil.oc2;

import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientSetup;
import li.cil.oc2.common.CommonSetup;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.container.ComputerContainer;
import li.cil.oc2.common.item.RISCVTesterItem;
import li.cil.oc2.common.tile.ComputerTileEntity;
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

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, API.MOD_ID);
    public static final RegistryObject<Item> RISCV_TESTER = ITEMS.register("riscv_tester", RISCVTesterItem::new);
    public static final RegistryObject<Item> COMPUTER_ITEM = ITEMS.register(Constants.COMPUTER_BLOCK_NAME, () -> new BlockItem(COMPUTER_BLOCK.get(), new Item.Properties().group(ITEM_GROUP)));

    public static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, API.MOD_ID);
    public static final RegistryObject<TileEntityType<ComputerTileEntity>> COMPUTER_TILE_ENTITY = TILES.register(Constants.COMPUTER_BLOCK_NAME, () -> TileEntityType.Builder.create(ComputerTileEntity::new, COMPUTER_BLOCK.get()).build(null));

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
    }
}
