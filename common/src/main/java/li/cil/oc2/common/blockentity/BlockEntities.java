/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;

public final class BlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = RegistryUtils.getInitializerFor(Registries.BLOCK_ENTITY_TYPE);

    private static final List<RegistrySupplier<? extends BlockEntityType<?>>> ALL = new ArrayList<>();

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<BlockEntityType<BusCableBlockEntity>> BUS_CABLE = register(Blocks.BUS_CABLE, BusCableBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<ChargerBlockEntity>> CHARGER = register(Blocks.CHARGER, ChargerBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<ComputerBlockEntity>> COMPUTER = register(Blocks.COMPUTER, ComputerBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<CreativeEnergyBlockEntity>> CREATIVE_ENERGY = register(Blocks.CREATIVE_ENERGY, CreativeEnergyBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<DiskDriveBlockEntity>> DISK_DRIVE = register(Blocks.DISK_DRIVE, DiskDriveBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<FlashDriveBlockEntity>> FLASH_DRIVE = register(Blocks.FLASH_DRIVE, FlashDriveBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<KeyboardBlockEntity>> KEYBOARD = register(Blocks.KEYBOARD, KeyboardBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<NetworkConnectorBlockEntity>> NETWORK_CONNECTOR = register(Blocks.NETWORK_CONNECTOR, NetworkConnectorBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<NetworkHubBlockEntity>> NETWORK_HUB = register(Blocks.NETWORK_HUB, NetworkHubBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<ProjectorBlockEntity>> PROJECTOR = register(Blocks.PROJECTOR, ProjectorBlockEntity::new);
    public static final RegistrySupplier<BlockEntityType<RedstoneInterfaceBlockEntity>> REDSTONE_INTERFACE = register(Blocks.REDSTONE_INTERFACE, RedstoneInterfaceBlockEntity::new);

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }

    public static List<BlockEntityType<?>> getAll() {
        return ALL.stream().<BlockEntityType<?>>map(RegistrySupplier::get).toList();
    }

    // --------------------------------------------------------------------- //

    @SuppressWarnings("ConstantConditions") // .build(null) is fine
    private static <B extends Block, T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> register(final RegistrySupplier<B> block, final BlockEntityType.BlockEntitySupplier<T> factory) {
        final RegistrySupplier<BlockEntityType<T>> type = BLOCK_ENTITIES.register(
            block.getId().getPath(), () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        ALL.add(type);
        return type;
    }
}
