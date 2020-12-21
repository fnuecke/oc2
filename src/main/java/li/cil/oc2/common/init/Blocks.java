package li.cil.oc2.common.init;

import li.cil.oc2.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.block.RedstoneInterfaceBlock;
import li.cil.oc2.common.block.ScreenBlock;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;

public final class Blocks {
    public static final Block COMPUTER_BLOCK = new ComputerBlock();
    public static final Block BUS_CABLE_BLOCK = new BusCableBlock();
    public static final Block REDSTONE_INTERFACE_BLOCK = new RedstoneInterfaceBlock();
    public static final Block SCREEN_BLOCK = new ScreenBlock();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        Registry.register(Registry.BLOCK, Constants.COMPUTER_BLOCK_NAME, COMPUTER_BLOCK);
        Registry.register(Registry.BLOCK, Constants.BUS_CABLE_BLOCK_NAME, BUS_CABLE_BLOCK);
        Registry.register(Registry.BLOCK, Constants.REDSTONE_INTERFACE_BLOCK_NAME, REDSTONE_INTERFACE_BLOCK);
        Registry.register(Registry.BLOCK, Constants.SCREEN_BLOCK_NAME, SCREEN_BLOCK);
    }
}
