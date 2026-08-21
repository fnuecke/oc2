/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.ModBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.entity.Entities;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

@EventBusSubscriber(modid = API.MOD_ID)
public final class CapabilityRegistrationNeoForge {
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

    @SubscribeEvent
    public static void handleRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        for (final BlockEntityType<?> type : BlockEntities.getAll()) {
            for (final CapabilityType<?> capability : BLOCK_ENTITY_CAPABILITIES) {
                registerBlockEntity(event, type, capability);
            }
            registerInteropBlockEntity(event, type);
        }

        for (final CapabilityType<?> capability : ENTITY_CAPABILITIES) {
            registerEntity(event, capability);
        }
        registerInteropEntity(event);
    }

    // --------------------------------------------------------------------- //

    private static <T> void registerEntity(final RegisterCapabilitiesEvent event, final CapabilityType<T> capability) {
        event.registerEntity(CapabilitiesImpl.entity(capability), Entities.ROBOT.get(),
            (robot, side) -> robot.getCapability(capability, side));
    }

    private static void registerInteropEntity(final RegisterCapabilitiesEvent event) {
        event.registerEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ENTITY, Entities.ROBOT.get(),
            (robot, side) -> {
                final var energy = robot.getCapability(Capabilities.ENERGY_STORAGE, side);
                return energy != null ? NeoForgeCapabilityAdapters.toNeoForge(energy) : null;
            });

        event.registerEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY_AUTOMATION, Entities.ROBOT.get(),
            (robot, side) -> {
                final var items = robot.getCapability(Capabilities.ITEM_HANDLER, side);
                return items != null ? NeoForgeCapabilityAdapters.toNeoForge(items) : null;
            });

        event.registerEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY, Entities.ROBOT.get(),
            (robot, context) -> {
                final var items = robot.getCapability(Capabilities.ITEM_HANDLER, null);
                return items != null ? NeoForgeCapabilityAdapters.toNeoForge(items) : null;
            });
    }

    private static <B extends net.minecraft.world.level.block.entity.BlockEntity> void registerInteropBlockEntity(
        final RegisterCapabilitiesEvent event, final BlockEntityType<B> type) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, type,
            (blockEntity, side) -> {
                if (!(blockEntity instanceof final ModBlockEntity modBlockEntity)) {
                    return null;
                }
                final var energy = modBlockEntity.getCapability(Capabilities.ENERGY_STORAGE, side);
                return energy != null ? NeoForgeCapabilityAdapters.toNeoForge(energy) : null;
            });

        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, type,
            (blockEntity, side) -> {
                if (!(blockEntity instanceof final ModBlockEntity modBlockEntity)) {
                    return null;
                }
                final var items = modBlockEntity.getCapability(Capabilities.ITEM_HANDLER, side);
                return items != null ? NeoForgeCapabilityAdapters.toNeoForge(items) : null;
            });
    }

    private static <T, B extends net.minecraft.world.level.block.entity.BlockEntity> void registerBlockEntity(
        final RegisterCapabilitiesEvent event, final BlockEntityType<B> type, final CapabilityType<T> capability) {
        event.registerBlockEntity(CapabilitiesImpl.block(capability), type,
            (blockEntity, side) -> blockEntity instanceof final ModBlockEntity modBlockEntity
                ? modBlockEntity.getCapability(capability, side)
                : null);
    }

    private CapabilityRegistrationNeoForge() {
    }
}
