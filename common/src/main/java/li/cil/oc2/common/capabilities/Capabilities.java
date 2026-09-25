/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import dev.architectury.injectables.annotations.ExpectPlatform;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public final class Capabilities {
    // Interop capabilities
    public static final CapabilityType<EnergyHandler> ENERGY_STORAGE = type("energy_storage", EnergyHandler.class);
    public static final CapabilityType<ItemHandler> ITEM_HANDLER = type("item_handler", ItemHandler.class);
    public static final CapabilityType<FluidHandler> FLUID_HANDLER = type("fluid_handler", FluidHandler.class);

    // Owned capabilities
    public static final CapabilityType<DeviceBusElement> DEVICE_BUS_ELEMENT = type("device_bus_element", DeviceBusElement.class);
    public static final CapabilityType<Device> DEVICE = type("device", Device.class);
    public static final CapabilityType<RedstoneEmitter> REDSTONE_EMITTER = type("redstone_emitter", RedstoneEmitter.class);
    public static final CapabilityType<NetworkInterface> NETWORK_INTERFACE = type("network_interface", NetworkInterface.class);
    public static final CapabilityType<TerminalUserProvider> TERMINAL_USER_PROVIDER = type("terminal_user_provider", TerminalUserProvider.class);
    public static final CapabilityType<Robot> ROBOT = type("robot", Robot.class);

    // --------------------------------------------------------------------- //

    @ExpectPlatform
    @Nullable
    public static <T> T get(final Level level, final BlockPos pos, final CapabilityType<T> type, @Nullable final Direction side) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static <T> T get(final BlockEntity blockEntity, final CapabilityType<T> type, @Nullable final Direction side) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static <T> T get(final ItemStack stack, final CapabilityType<T> type) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static <T> T get(final Entity entity, final CapabilityType<T> type, @Nullable final Direction side) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> Invalidatable<T> watch(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side,
                                             final CapabilityType<T> type) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void invalidate(final BlockEntity blockEntity) {
        throw new AssertionError();
    }

    // --------------------------------------------------------------------- //

    private static <T> CapabilityType<T> type(final String name, final Class<T> type) {
        return new CapabilityType<>(ResourceLocation.fromNamespaceAndPath(API.MOD_ID, name), type);
    }

    private Capabilities() {
    }
}
