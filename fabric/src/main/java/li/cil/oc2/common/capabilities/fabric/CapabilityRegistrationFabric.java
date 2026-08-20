/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import li.cil.oc2.api.fabric.EnergyStorage;
import li.cil.oc2.api.fabric.ItemStorage;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;

public final class CapabilityRegistrationFabric {
    private static final List<CapabilityType<?>> BLOCK_ENTITY_CAPABILITIES = List.of(
            Capabilities.DEVICE_BUS_ELEMENT,
            Capabilities.DEVICE,
            Capabilities.REDSTONE_EMITTER,
            Capabilities.NETWORK_INTERFACE,
            Capabilities.TERMINAL_USER_PROVIDER
    );

    private static final List<CapabilityType<?>> ENTITY_CAPABILITIES = List.of(
            Capabilities.ROBOT,
            Capabilities.TERMINAL_USER_PROVIDER
    );

    // ------------------------------------------------------------- //

    public static void initialize() {
        CapabilityWatchers.initialize();

        final BlockEntityType<?>[] types = BlockEntities.getAll().toArray(new BlockEntityType<?>[0]);

        for (final CapabilityType<?> capability : BLOCK_ENTITY_CAPABILITIES) {
            registerBlockEntity(capability, types);
        }
        registerInteropBlockEntities(types);

        for (final CapabilityType<?> capability : ENTITY_CAPABILITIES) {
            registerEntity(capability);
        }
        registerInteropEntity();
    }

    // ------------------------------------------------------------- //

    private static <T> void registerBlockEntity(final CapabilityType<T> capability, final BlockEntityType<?>[] types) {
        CapabilitiesImpl.block(capability).registerForBlockEntities(
                (blockEntity, side) -> blockEntity instanceof final ModBlockEntity modBlockEntity
                        ? modBlockEntity.getCapability(capability, side)
                        : null,
                types);
    }

    private static void registerInteropBlockEntities(final BlockEntityType<?>[] types) {
        EnergyStorage.SIDED.registerForBlockEntities((blockEntity, side) -> {
            if (!(blockEntity instanceof final ModBlockEntity modBlockEntity)) {
                return null;
            }
            final var energy = modBlockEntity.getCapability(Capabilities.ENERGY_STORAGE, side);
            return energy != null ? FabricCapabilityAdapters.toFabric(energy) : null;
        }, types);

        ItemStorage.SIDED.registerForBlockEntities((blockEntity, side) -> {
            if (!(blockEntity instanceof final ModBlockEntity modBlockEntity)) {
                return null;
            }
            final var items = modBlockEntity.getCapability(Capabilities.ITEM_HANDLER, side);
            return items != null ? FabricCapabilityAdapters.toFabric(items) : null;
        }, types);
    }

    private static <T> void registerEntity(final CapabilityType<T> capability) {
        CapabilitiesImpl.entity(capability).registerForType(
                (final Robot robot, final Direction side) -> robot.getCapability(capability, side),
                Entities.ROBOT.get());
    }

    private static void registerInteropEntity() {
        EnergyStorage.ENTITY.registerForType((final Robot robot, final Direction side) -> {
            final var energy = robot.getCapability(Capabilities.ENERGY_STORAGE, side);
            return energy != null ? FabricCapabilityAdapters.toFabric(energy) : null;
        }, Entities.ROBOT.get());

        ItemStorage.ENTITY.registerForType((final Robot robot, final Direction side) -> {
            final var items = robot.getCapability(Capabilities.ITEM_HANDLER, side);
            return items != null ? FabricCapabilityAdapters.toFabric(items) : null;
        }, Entities.ROBOT.get());
    }

    private CapabilityRegistrationFabric() {
    }
}
