/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.tis3d;

import li.cil.oc2.api.API;
import li.cil.tis3d.api.serial.SerialInterface;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class SerialInterfaceProviderImpl implements SerialInterfaceProvider {
    private static final SerialProtocolDocumentationReference DOCUMENTATION = new SerialProtocolDocumentationReference(
        Component.translatable("manual." + API.MOD_ID + ".serial_protocols.serial_interface"), "oc2_serial_interface.md");

    // --------------------------------------------------------------------- //

    @Override
    public boolean matches(final Level level, final BlockPos position, final Direction side) {
        return TIS3D.portFor(level, position, side) != null;
    }

    @Override
    public Optional<SerialInterface> getInterface(final Level level, final BlockPos position, final Direction side) {
        return Optional.ofNullable(TIS3D.portFor(level, position, side)).map(SerialInterfaceImpl::new);
    }

    @Override
    public Optional<SerialProtocolDocumentationReference> getDocumentationReference() {
        return Optional.of(DOCUMENTATION);
    }

    @Override
    public boolean stillValid(final Level level, final BlockPos position, final Direction side, final SerialInterface serialInterface) {
        return serialInterface instanceof final SerialInterfaceImpl connectorInterface
            && connectorInterface.matchesPort(TIS3D.portFor(level, position, side));
    }
}
