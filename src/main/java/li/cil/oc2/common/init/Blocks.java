package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.block.RedstoneInterfaceBlock;
import li.cil.oc2.common.block.ScreenBlock;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final RegistryObject<Block> COMPUTER_BLOCK = BLOCKS.register(Constants.COMPUTER_BLOCK_NAME, ComputerBlock::new);
    public static final RegistryObject<Block> BUS_CABLE_BLOCK = BLOCKS.register(Constants.BUS_CABLE_BLOCK_NAME, BusCableBlock::new);
    public static final RegistryObject<Block> REDSTONE_INTERFACE_BLOCK = BLOCKS.register(Constants.REDSTONE_INTERFACE_BLOCK_NAME, RedstoneInterfaceBlock::new);
    public static final RegistryObject<Block> SCREEN_BLOCK = BLOCKS.register(Constants.SCREEN_BLOCK_NAME, ScreenBlock::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
