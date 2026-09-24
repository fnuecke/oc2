/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

public final class SerialPorts {
    private final Map<BlockEntity, SerialPort[]> ports = new WeakHashMap<>();
    private final int defaultBaudRate;

    // --------------------------------------------------------------------- //

    public SerialPorts(final int defaultBaudRate) {
        this.defaultBaudRate = defaultBaudRate;
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public SerialPort get(final BlockEntity blockEntity, @Nullable final Direction side) {
        final Level level = blockEntity.getLevel();
        if (side == null || level == null) {
            return null;
        }

        final SerialPort[] sides = ports.computeIfAbsent(blockEntity, unused -> new SerialPort[Constants.BLOCK_FACE_COUNT]);
        SerialPort port = sides[side.get3DDataValue()];
        if (port == null) {
            port = new SerialPort(LevelUtils.gameTimeSupplier(level));
            port.setConfiguration(port.getAddress(), defaultBaudRate);
            sides[side.get3DDataValue()] = port;
        }
        return port;
    }
}
