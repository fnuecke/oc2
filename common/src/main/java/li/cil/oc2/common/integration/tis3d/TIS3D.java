/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.tis3d;

import dev.architectury.registry.registries.DeferredRegister;
import li.cil.oc2.api.API;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.integration.ModIntegration;
import li.cil.oc2.common.serial.SerialEndpoint;
import li.cil.oc2.common.serial.SerialPort;
import li.cil.oc2.common.serial.SerialPorts;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
import java.util.Objects;

public final class TIS3D {
    private static final SerialPorts PORTS = new SerialPorts(SerialEndpoint.MIN_BAUD_RATE);

    // --------------------------------------------------------------------- //

    public static void registerProviders() {
        final DeferredRegister<SerialInterfaceProvider> providers = DeferredRegister.create(API.MOD_ID, SerialInterfaceProvider.REGISTRY);
        providers.register("serial_interface", SerialInterfaceProviderImpl::new);
        providers.register();
    }

    public static BlockEntityType<?> casingBlockEntityType() {
        return Objects.requireNonNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(ModIntegration.TIS3D.modId(), "casing")));
    }

    @Nullable
    public static NetworkInterface networkInterfaceFor(final BlockEntity casing, @Nullable final Direction side) {
        return PORTS.get(casing, side);
    }

    @Nullable
    static SerialPort portFor(final Level level, final BlockPos connectorPosition, final Direction side) {
        if (!(level.getBlockEntity(connectorPosition) instanceof final NetworkConnectorBlockEntity connector)
            || !connector.isValid()
            || side != NetworkConnectorBlock.getFacing(connector.getBlockState()).getOpposite()) {
            return null;
        }

        final BlockEntity casing = level.getBlockEntity(connectorPosition.relative(side));
        return casing != null ? PORTS.get(casing, side.getOpposite()) : null;
    }

    private TIS3D() {
    }
}
