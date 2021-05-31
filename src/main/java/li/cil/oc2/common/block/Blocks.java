package li.cil.oc2.common.block;

import li.cil.oc2.api.API;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class Blocks {

    public static ComputerBlock COMPUTER;
    public static BusCableBlock BUS_CABLE;
    public static NetworkConnectorBlock NETWORK_CONNECTOR;
    public static NetworkHubBlock NETWORK_HUB;
    public static RedstoneInterfaceBlock REDSTONE_INTERFACE;
    public static DiskDriveBlock DISK_DRIVE;
    public static ChargerBlock CHARGER;
    public static CreativeEnergyBlock CREATIVE_ENERGY;

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        COMPUTER = register("computer", new ComputerBlock());
        BUS_CABLE = register("bus_cable", new BusCableBlock());
        NETWORK_CONNECTOR = register("network_connector", new NetworkConnectorBlock());
        NETWORK_HUB = register("network_hub", new NetworkHubBlock());
        REDSTONE_INTERFACE = register("redstone_interface", new RedstoneInterfaceBlock());
        DISK_DRIVE = register("disk_drive", new DiskDriveBlock());
        CHARGER = register("charger", new ChargerBlock());
        CREATIVE_ENERGY = register("creative_energy", new CreativeEnergyBlock());
    }

    private static <T extends Block> T register(String id, T block) {
        Registry.register(Registry.BLOCK, new ResourceLocation(API.MOD_ID, id), block);
        return block;
    }
}
