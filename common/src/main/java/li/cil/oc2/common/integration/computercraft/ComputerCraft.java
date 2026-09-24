/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.integration.ModIntegration;
import li.cil.oc2.common.serial.SerialEndpoint;
import li.cil.oc2.common.serial.SerialPort;
import li.cil.oc2.common.serial.SerialPorts;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ComputerCraft {
    private static final Set<ResourceLocation> COMPUTER_TYPE_IDS = Stream.of("computer_normal", "computer_advanced", "computer_command")
        .map(name -> ResourceLocation.fromNamespaceAndPath(ModIntegration.COMPUTERCRAFT.modId(), name))
        .collect(Collectors.toUnmodifiableSet());
    private static final SerialPorts PORTS = new SerialPorts(SerialEndpoint.DEFAULT_BAUD_RATE);

    @Nullable
    private static Set<BlockEntityType<?>> computerTypes;

    // --------------------------------------------------------------------- //

    public static Set<BlockEntityType<?>> computerTypes() {
        if (computerTypes == null) {
            computerTypes = COMPUTER_TYPE_IDS.stream()
                .<BlockEntityType<?>>map(BuiltInRegistries.BLOCK_ENTITY_TYPE::get)
                .collect(Collectors.toUnmodifiableSet());
        }
        return computerTypes;
    }

    @Nullable
    public static NetworkInterface networkInterfaceFor(final BlockEntity blockEntity, @Nullable final Direction side) {
        return PORTS.get(blockEntity, side);
    }

    @Nullable
    public static IPeripheral peripheralFor(final NetworkConnectorBlockEntity connector, @Nullable final Direction side) {
        final Level level = connector.getLevel();
        if (level == null || !connector.isValid() || side != NetworkConnectorBlock.getFacing(connector.getBlockState()).getOpposite()) {
            return null;
        }

        final NetworkInterface computerInterface = Capabilities.get(level, connector.getBlockPos().relative(side), Capabilities.NETWORK_INTERFACE, side.getOpposite());
        return computerInterface instanceof final SerialPort port ? new SerialPeripheral(connector, port) : null;
    }

    private ComputerCraft() {
    }
}
