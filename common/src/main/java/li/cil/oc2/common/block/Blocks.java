/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.API;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

import java.util.function.Function;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = RegistryUtils.getInitializerFor(Registries.BLOCK);

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<BusCableBlock> BUS_CABLE = register("bus_cable", BusCableBlock::new);
    public static final RegistrySupplier<ChargerBlock> CHARGER = register("charger", ChargerBlock::new);
    public static final RegistrySupplier<ComputerBlock> COMPUTER = register("computer", ComputerBlock::new);
    public static final RegistrySupplier<CreativeEnergyBlock> CREATIVE_ENERGY = register("creative_energy", CreativeEnergyBlock::new);
    public static final RegistrySupplier<DiskDriveBlock> DISK_DRIVE = register("disk_drive", DiskDriveBlock::new);
    public static final RegistrySupplier<FlashDriveBlock> FLASH_DRIVE = register("flash_drive", FlashDriveBlock::new);
    public static final RegistrySupplier<KeyboardBlock> KEYBOARD = register("keyboard", KeyboardBlock::new);
    public static final RegistrySupplier<NetworkConnectorBlock> NETWORK_CONNECTOR = register("network_connector", NetworkConnectorBlock::new);
    public static final RegistrySupplier<NetworkHubBlock> NETWORK_HUB = register("network_hub", NetworkHubBlock::new);
    public static final RegistrySupplier<InternetGatewayBlock> INTERNET_GATEWAY = register("internet_gateway", InternetGatewayBlock::new);
    public static final RegistrySupplier<ProjectorBlock> PROJECTOR = register("projector", ProjectorBlock::new);
    public static final RegistrySupplier<RedstoneInterfaceBlock> REDSTONE_INTERFACE = register("redstone_interface", RedstoneInterfaceBlock::new);
    public static final RegistrySupplier<TerminalBlock> TERMINAL = register("terminal", TerminalBlock::new);
    public static final RegistrySupplier<TransposerBlock> TRANSPOSER = register("transposer", TransposerBlock::new);

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }

    // --------------------------------------------------------------------- //

    private static <T extends Block> RegistrySupplier<T> register(final String name, final Function<Properties, T> factory) {
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(API.MOD_ID, name));
        return BLOCKS.register(name, () -> factory.apply(Properties.of().setId(key)));
    }
}
