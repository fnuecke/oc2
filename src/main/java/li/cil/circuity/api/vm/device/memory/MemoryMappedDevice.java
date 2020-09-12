package li.cil.circuity.api.vm.device.memory;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link MemoryMappedDevice}s can be registered with a {@link MemoryMap}
 * so that they can be accessed via a memory range using the same mechanisms used for accessing RAM.
 */
public interface MemoryMappedDevice extends Device {
    Logger LOGGER = LogManager.getLogger();

    int getLength();

    default byte load8(final int offset) throws MemoryAccessException {
        LOGGER.debug("Unsupported device read access in [{}] at [{}].", getClass().getSimpleName(), offset);
        return 0;
    }

    default void store8(final int offset, final byte value) throws MemoryAccessException {
        LOGGER.debug("Unsupported device write access in [{}] at [{}].", getClass().getSimpleName(), offset);
    }

    default short load16(final int offset) throws MemoryAccessException {
        LOGGER.debug("Unsupported device read access in [{}] at [{}].", getClass().getSimpleName(), offset);
        return 0;
    }

    default void store16(final int offset, final short value) throws MemoryAccessException {
        LOGGER.debug("Unsupported device write access in [{}] at [{}].", getClass().getSimpleName(), offset);
    }

    default int load32(final int offset) throws MemoryAccessException {
        LOGGER.debug("Unsupported device read access in [{}] at [{}].", getClass().getSimpleName(), offset);
        return 0;
    }

    default void store32(final int offset, final int value) throws MemoryAccessException {
        LOGGER.debug("Unsupported device write access in [{}] at [{}].", getClass().getSimpleName(), offset);
    }
}
