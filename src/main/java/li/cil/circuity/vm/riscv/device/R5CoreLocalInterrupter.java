package li.cil.circuity.vm.riscv.device;

import li.cil.circuity.api.vm.Interrupt;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.Steppable;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.device.rtc.RealTimeCounter;
import li.cil.circuity.vm.riscv.R5;

import java.util.Arrays;

/**
 * See: https://github.com/riscv/riscv-isa-sim/blob/master/riscv/clint.cc
 * <pre>
 * // 0000 msip hart 0
 * // 0004 msip hart 1
 * 4000 mtimecmp hart 0 lo
 * 4004 mtimecmp hart 0 hi
 * bff8 mtime lo
 * bffc mtime hi
 * </pre>
 */
public final class R5CoreLocalInterrupter implements Steppable, InterruptSource, MemoryMappedDevice {
    private final RealTimeCounter rtc;
    private long mtimecmp = -1;

    private final Interrupt msip = new Interrupt(R5.MSIP_SHIFT);
    private final Interrupt mtip = new Interrupt(R5.MTIP_SHIFT);

    public R5CoreLocalInterrupter(final RealTimeCounter rtc) {
        this.rtc = rtc;
    }

    public Interrupt getMachineSoftwareInterrupt() {
        return msip;
    }

    public Interrupt getMachineTimerInterrupt() {
        return mtip;
    }

    @Override
    public void step(final int cycles) {
        checkInterrupt();
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return Arrays.asList(msip, mtip);
    }

    @Override
    public int getLength() {
        return 0x000C0000;
    }

    @Override
    public int load32(final int offset) {
        switch (offset) {
            case 0x4000: {
                return (int) mtimecmp;
            }
            case 0x4004: {
                return (int) (mtimecmp >>> 32);
            }
            case 0xBFF8: {
                return (int) rtc.getTime();
            }
            case 0xBFFC: {
                return (int) (rtc.getTime() >> 32);
            }
            default: {
                return 0;
            }
        }
    }

    @Override
    public void store32(final int offset, final int value) {
        switch (offset) {
            case 0x4000: {
                mtimecmp = (mtimecmp & ~0xFFFFFFFFL) | (value & 0xFFFFFFFFL);
                if (!checkInterrupt()) {
                    mtip.lowerInterrupt();
                }
                break;
            }
            case 0x4004: {
                mtimecmp = (mtimecmp & 0xFFFFFFFFL) | ((long) value << 32);
                if (!checkInterrupt()) {
                    mtip.lowerInterrupt();
                }
                break;
            }
        }
    }

    private boolean checkInterrupt() {
        if (Long.compareUnsigned(mtimecmp, rtc.getTime()) <= 0) {
            mtip.raiseInterrupt();
            return true;
        }

        return false;
    }
}
