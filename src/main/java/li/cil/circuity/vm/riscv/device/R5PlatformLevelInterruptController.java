package li.cil.circuity.vm.riscv.device;

import li.cil.circuity.api.vm.Interrupt;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.Interrupter;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.vm.components.InterruptSourceRegistry;
import li.cil.circuity.vm.riscv.R5;

import java.util.Arrays;

/**
 * Implementation of a PLIC with 32 sources.
 * <p>
 * See: https://github.com/riscv/riscv-plic-spec/blob/master/riscv-plic.adoc
 * See: https://github.com/riscv/opensbi/blob/master/lib/utils/irqchip/plic.c
 * <pre>
 * base + 0x000000: Reserved (interrupt source 0 does not exist)
 * base + 0x000004: Interrupt source 1 priority
 * base + 0x000008: Interrupt source 2 priority
 * ...
 * base + 0x000FFC: Interrupt source 1023 priority
 * base + 0x001000: Interrupt Pending bit 0-31
 * base + 0x00107C: Interrupt Pending bit 992-1023
 * ...
 * base + 0x002000: Enable bits for sources 0-31 on context 0
 * base + 0x002004: Enable bits for sources 32-63 on context 0
 * ...
 * base + 0x00207F: Enable bits for sources 992-1023 on context 0
 * base + 0x002080: Enable bits for sources 0-31 on context 1
 * base + 0x002084: Enable bits for sources 32-63 on context 1
 * ...
 * base + 0x0020FF: Enable bits for sources 992-1023 on context 1
 * base + 0x002100: Enable bits for sources 0-31 on context 2
 * base + 0x002104: Enable bits for sources 32-63 on context 2
 * ...
 * base + 0x00217F: Enable bits for sources 992-1023 on context 2
 * ...
 * base + 0x1F1F80: Enable bits for sources 0-31 on context 15871
 * base + 0x1F1F84: Enable bits for sources 32-63 on context 15871
 * base + 0x1F1FFF: Enable bits for sources 992-1023 on context 15871
 * ...
 * base + 0x1FFFFC: Reserved
 * base + 0x200000: Priority threshold for context 0
 * base + 0x200004: Claim/complete for context 0
 * base + 0x200008: Reserved
 * ...
 * base + 0x200FFC: Reserved
 * base + 0x201000: Priority threshold for context 1
 * base + 0x201004: Claim/complete for context 1
 * ...
 * base + 0x3FFE000: Priority threshold for context 15871
 * base + 0x3FFE004: Claim/complete for context 15871
 * base + 0x3FFE008: Reserved
 * ...
 * base + 0x3FFFFFC: Reserved
 * </pre>
 */
public class R5PlatformLevelInterruptController implements MemoryMappedDevice, Interrupter, InterruptSource {
    private static final int PLIC_PRIORITY_BASE = 0x0;
    private static final int PLIC_PENDING_BASE = 0x1000;
    private static final int PLIC_ENABLE_BASE = 0x2000;
    private static final int PLIC_ENABLE_STRIDE = 0x80;
    private static final int PLIC_CONTEXT_BASE = 0x200000;
    private static final int PLIC_CONTEXT_STRIDE = 0x1000;

    private final InterruptSourceRegistry interrupts = new InterruptSourceRegistry();
    private int pending;
    private int served;

    private final Interrupt meip = new Interrupt(R5.MEIP_SHIFT);
    private final Interrupt seip = new Interrupt(R5.SEIP_SHIFT);

    public Interrupt getMachineExternalInterrupt() {
        return meip;
    }

    public Interrupt getSupervisorExternalInterrupt() {
        return seip;
    }

    @Override
    public int getLength() {
        return 0x04000000;
    }

    @Override
    public int load(final int offset, final int sizeLog2) {
        assert sizeLog2 == 2;
        switch (offset) {
            // 0x0: Reserved.
            // 0x1 - 0x000FFC: Priorities; hardcoded to zero.
            case PLIC_PENDING_BASE: { // Pending bits 0-31.
                return pending & ~served;
            }
            case PLIC_ENABLE_BASE: { // Enable bits 0-31 on context 0 (hart 0).
                return 0xFFFFFFFF; // Hardcoded to enabled.
            }
            // PLIC_CONTEXT_BASE: Priority threshold on context 0; hardcoded to zero.
            case PLIC_CONTEXT_BASE + 4: { // Claim/complete for context 0
                return claim();
            }
            default: {
                return 0;
            }
        }
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) {
        assert sizeLog2 == 2;
        switch (offset) {
            case PLIC_CONTEXT_BASE + 4: { // Claim/complete for context 0
                complete(value);
                break;
            }
        }
    }

    @Override
    public void raiseInterrupts(final int mask) {
        pending |= mask;
        propagate();
    }

    @Override
    public void lowerInterrupts(final int mask) {
        pending &= ~mask;
        propagate();
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return Arrays.asList(meip, seip);
    }

    @Override
    public int registerInterrupt() {
        return interrupts.registerInterrupt();
    }

    @Override
    public boolean registerInterrupt(final int id) {
        return interrupts.registerInterrupt(id);
    }

    @Override
    public void releaseInterrupt(final int id) {
        interrupts.releaseInterrupt(id);
    }

    private int claim() {
        final int unserved = pending & ~served;
        if (unserved == 0) {
            return 0;
        }

        final int index = Integer.numberOfTrailingZeros(unserved);

        served |= 1 << index;
        propagate();

        return index + 1;
    }

    private void complete(final int value) {
        if (value <= 0) {
            return;
        }

        final int index = value - 1;
        if (index >= 32) {
            return;
        }

        served &= ~(1 << index);
        propagate();
    }

    private void propagate() {
        final int mask = pending & ~served;
        if (mask != 0) {
            meip.raiseInterrupt();
            seip.raiseInterrupt();
        } else {
            meip.lowerInterrupt();
            seip.lowerInterrupt();
        }
    }
}
