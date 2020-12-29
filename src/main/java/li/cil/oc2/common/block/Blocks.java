package li.cil.oc2.common.block;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<ComputerBlock> COMPUTER_BLOCK = BLOCKS.register(Constants.COMPUTER_BLOCK_NAME, ComputerBlock::new);
    public static final RegistryObject<BusCableBlock> BUS_CABLE_BLOCK = BLOCKS.register(Constants.BUS_CABLE_BLOCK_NAME, BusCableBlock::new);
    public static final RegistryObject<RedstoneInterfaceBlock> REDSTONE_INTERFACE_BLOCK = BLOCKS.register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, RedstoneInterfaceBlock::new);
    public static final RegistryObject<ScreenBlock> SCREEN_BLOCK = BLOCKS.register(Constants.SCREEN_BLOCK_NAME, ScreenBlock::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
