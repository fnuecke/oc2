/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.world.level.block.Block;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = RegistryUtils.getInitializerFor(Registries.BLOCK);

    ///////////////////////////////////////////////////////////////////

    public static final RegistrySupplier<BusCableBlock> BUS_CABLE = BLOCKS.register("bus_cable", BusCableBlock::new);
    public static final RegistrySupplier<ChargerBlock> CHARGER = BLOCKS.register("charger", ChargerBlock::new);
    public static final RegistrySupplier<ComputerBlock> COMPUTER = BLOCKS.register("computer", ComputerBlock::new);
    public static final RegistrySupplier<CreativeEnergyBlock> CREATIVE_ENERGY = BLOCKS.register("creative_energy", CreativeEnergyBlock::new);
    public static final RegistrySupplier<DiskDriveBlock> DISK_DRIVE = BLOCKS.register("disk_drive", DiskDriveBlock::new);
    public static final RegistrySupplier<KeyboardBlock> KEYBOARD = BLOCKS.register("keyboard", KeyboardBlock::new);
    public static final RegistrySupplier<NetworkConnectorBlock> NETWORK_CONNECTOR = BLOCKS.register("network_connector", NetworkConnectorBlock::new);
    public static final RegistrySupplier<NetworkHubBlock> NETWORK_HUB = BLOCKS.register("network_hub", NetworkHubBlock::new);
    public static final RegistrySupplier<ProjectorBlock> PROJECTOR = BLOCKS.register("projector", ProjectorBlock::new);
    public static final RegistrySupplier<RedstoneInterfaceBlock> REDSTONE_INTERFACE = BLOCKS.register("redstone_interface", RedstoneInterfaceBlock::new);

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
    }
}
