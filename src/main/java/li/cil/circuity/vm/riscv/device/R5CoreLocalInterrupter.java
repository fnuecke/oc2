package li.cil.circuity.vm.riscv.device;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import li.cil.circuity.api.vm.Interrupt;
import li.cil.circuity.api.vm.device.InterruptController;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.Steppable;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.api.vm.device.rtc.RealTimeCounter;
import li.cil.circuity.vm.riscv.R5;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of a shared CLINT that is aware of one or more harts.
 * <p>
 * See: https://github.com/riscv/riscv-isa-sim/blob/master/riscv/clint.cc
 */
public final class R5CoreLocalInterrupter implements Steppable, InterruptSource, MemoryMappedDevice {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int CLINT_SIP_BASE = 0x0000;
    private static final int CLINT_TIMECMP_BASE = 0x4000;
    private static final int CLINT_TIME_BASE = 0xBFF8;

    private final RealTimeCounter rtc;

    private final Int2ObjectMap<Interrupt> msips = new Int2ObjectArrayMap<>();
    private final Int2ObjectMap<Interrupt> mtips = new Int2ObjectArrayMap<>();
    private final Int2LongMap mtimecmps = new Int2LongArrayMap();

    public R5CoreLocalInterrupter(final RealTimeCounter rtc) {
        this.rtc = rtc;
    }

    public void putHart(final int id, final InterruptController interruptController) {
        final Interrupt msip = new Interrupt(R5.MSIP_SHIFT);
        msip.controller = interruptController;
        msips.put(id, msip);

        final Interrupt mtip = new Interrupt(R5.MTIP_SHIFT);
        mtip.controller = interruptController;
        mtips.put(id, mtip);

        mtimecmps.put(id, -1);
    }

    @Override
    public void step(final int cycles) {
        checkTimeComparators(); // TODO Polling sucks.
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return Stream.concat(msips.values().stream(), mtips.values().stream()).collect(Collectors.toList());
    }

    @Override
    public int getLength() {
        return 0x000C0000;
    }

    @Override
    public int load(final int offset, final int sizeLog2) {
        assert sizeLog2 == Sizes.SIZE_32_LOG2;

        if (offset >= CLINT_SIP_BASE && offset < CLINT_TIMECMP_BASE) {
            final int hartId = (offset - CLINT_SIP_BASE) >>> 2;
            if (msips.containsKey(hartId)) {
                if ((offset & 0b11) == 0) {
                    return msips.get(hartId).isRaised() ? 1 : 0;
                } else {
                    LOGGER.debug("invalid sip read [{}]", Integer.toHexString(offset));
                }
            } else {
                LOGGER.debug("invalid sip hartid [{}]", hartId);
            }

            return 0;
        } else if (offset >= CLINT_TIMECMP_BASE && offset < CLINT_TIME_BASE) {
            final int hartId = (offset - CLINT_TIMECMP_BASE) >>> 3;
            if (mtimecmps.containsKey(hartId)) {
                if ((offset & 0b111) == 0) {
                    final long mtimecmp = mtimecmps.get(hartId);
                    return (int) (mtimecmp);
                } else if ((offset & 0b111) == 4) {
                    final long mtimecmp = mtimecmps.get(hartId);
                    return (int) (mtimecmp >>> 32);
                } else {
                    LOGGER.debug("invalid timecmp read [{}]", Integer.toHexString(offset));
                }
            } else {
                LOGGER.debug("invalid timecmp hartid [{}]", hartId);
            }

            return 0;
        } else if (offset == CLINT_TIME_BASE) {
            return (int) rtc.getTime();
        } else if (offset == CLINT_TIME_BASE + 4) {
            return (int) (rtc.getTime() >>> 32);
        }

        LOGGER.debug("invalid read offset [{}]", Integer.toHexString(offset));
        return 0;
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) {
        assert sizeLog2 == Sizes.SIZE_32_LOG2;

        if (offset >= CLINT_SIP_BASE && offset < CLINT_TIMECMP_BASE) {
            final int hartId = (offset - CLINT_SIP_BASE) >>> 2;
            if (msips.containsKey(hartId)) {
                if ((offset & 0b11) == 0) {
                    if (value == 0) {
                        msips.get(hartId).lowerInterrupt();
                    } else {
                        msips.get(hartId).raiseInterrupt();
                    }
                } else {
                    LOGGER.debug("invalid sip write [{}]", Integer.toHexString(offset));
                }
            } else {
                LOGGER.debug("invalid sip hartid [{}]", hartId);
            }

            return;
        } else if (offset >= CLINT_TIMECMP_BASE && offset < CLINT_TIME_BASE) {
            final int hartId = (offset - CLINT_TIMECMP_BASE) >>> 3;
            if (mtimecmps.containsKey(hartId)) {
                if ((offset & 0b111) == 0) {
                    long mtimecmp = mtimecmps.get(hartId);
                    mtimecmp = (mtimecmp & ~0xFFFFFFFFL) | (value & 0xFFFFFFFFL);
                    mtimecmps.put(hartId, mtimecmp);

                    if (mtimecmp <= rtc.getTime()) {
                        mtips.get(hartId).raiseInterrupt();
                    } else {
                        mtips.get(hartId).lowerInterrupt();
                    }
                } else if ((offset & 0b111) == 4) {
                    long mtimecmp = mtimecmps.get(hartId);
                    mtimecmp = (mtimecmp & 0xFFFFFFFFL) | ((long) value << 32);
                    mtimecmps.put(hartId, mtimecmp);

                    if (mtimecmp <= rtc.getTime()) {
                        mtips.get(hartId).raiseInterrupt();
                    } else {
                        mtips.get(hartId).lowerInterrupt();
                    }
                } else {
                    LOGGER.debug("invalid timecmp write [{}]", Integer.toHexString(offset));
                }
            } else {
                LOGGER.debug("invalid timecmp hartid [{}]", hartId);
            }

            return;
        } else if (offset == CLINT_TIME_BASE) {
            LOGGER.debug("invalid time write");
            return;
        } else if (offset == CLINT_TIME_BASE + 4) {
            LOGGER.debug("invalid timeh write");
            return;
        }

        LOGGER.debug("invalid write offset [{}]", Integer.toHexString(offset));
    }

    private void checkTimeComparators() {
        mtimecmps.forEach((hartId, mtimecmp) -> {
            if (Long.compareUnsigned(mtimecmp, rtc.getTime()) <= 0) {
                mtips.get((int) hartId).raiseInterrupt();
            }
        });
    }
}
