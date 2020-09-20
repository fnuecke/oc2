package li.cil.circuity.vm.riscv.device;

import li.cil.circuity.api.vm.Interrupt;
import li.cil.circuity.api.vm.device.InterruptController;
import li.cil.circuity.api.vm.device.InterruptSource;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.vm.riscv.R5;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of a PLIC with 31 sources supporting a single hart. It provides external
 * interrupts for M and S levels.
 * <p>
 * See: https://github.com/riscv/riscv-plic-spec/blob/master/riscv-plic.adoc
 * See: https://github.com/riscv/opensbi/blob/master/lib/utils/irqchip/plic.c
 */
public class R5PlatformLevelInterruptController implements MemoryMappedDevice, InterruptController, InterruptSource {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PLIC_PRIORITY_BASE = 0x000004;
    private static final int PLIC_PENDING_BASE = 0x001000;
    private static final int PLIC_ENABLE_BASE = 0x002000;
    private static final int PLIC_ENABLE_STRIDE = 0x80;
    private static final int PLIC_CONTEXT_BASE = 0x200000;
    private static final int PLIC_CONTEXT_STRIDE = 0x1000;
    private static final int PLIC_LENGTH = 0x04000000;

    private static final int PLIC_SOURCE_COUNT = 32; // Includes always off zero!
    private static final int PLIC_CONTEXT_COUNT = 2; // MEIP and SEIP for one hart.
    private static final int PLIC_MAX_PRIORITY = 7; // Number of priority level supported. Must have all bits set.

    private final Interrupt meip = new Interrupt(R5.MEIP_SHIFT);
    private final Interrupt seip = new Interrupt(R5.SEIP_SHIFT);

    private final int sourceWords; // Size of blocks holding flags for sources in words.
    private final int[] priorityBySource;
    private final int[] thresholdByContext;
    private final Interrupt[] interruptByContext = {meip, seip};
    private final AtomicInteger[] pending;
    private final AtomicInteger[] claimed;
    private final int[] enabled; // Contiguous words for all sources and all contexts (c0:s0...c0:sN,...,cM:s0...cM:N)

    public R5PlatformLevelInterruptController() {
        sourceWords = (PLIC_SOURCE_COUNT + 31) >>> 5;
        priorityBySource = new int[PLIC_SOURCE_COUNT];
        thresholdByContext = new int[PLIC_CONTEXT_COUNT];
        pending = new AtomicInteger[sourceWords];
        for (int i = 0; i < sourceWords; i++) {
            pending[i] = new AtomicInteger(0);
        }
        claimed = new AtomicInteger[sourceWords];
        for (int i = 0; i < sourceWords; i++) {
            claimed[i] = new AtomicInteger(0);
        }
        enabled = new int[sourceWords * PLIC_CONTEXT_COUNT];
    }

    public void setHart(final InterruptController interruptController) {
        for (final Interrupt interrupt : interruptByContext) {
            interrupt.controller = interruptController;
        }
    }

    @Override
    public int getLength() {
        return PLIC_LENGTH;
    }

    @Override
    public int load(final int offset, final int sizeLog2) {
        assert sizeLog2 == 2;

        if (offset >= PLIC_PRIORITY_BASE && offset < PLIC_PRIORITY_BASE + (PLIC_SOURCE_COUNT << 2)) {
            // base + 0x000004: Interrupt source 1 priority
            // base + 0x000008: Interrupt source 2 priority
            // ...
            // base + 0x000FFC: Interrupt source 1023 priority

            final int source = ((offset - PLIC_PRIORITY_BASE) >> 2) + 1; // Plus one because we skip zero.
            return priorityBySource[source];
        } else if (offset >= PLIC_PENDING_BASE && offset < PLIC_PENDING_BASE + sourceWords) {
            // base + 0x001000: Interrupt Pending bit 0-31
            // base + 0x00107C: Interrupt Pending bit 992-1023
            // ...

            final int word = (offset - PLIC_PENDING_BASE) >> 2;
            return pending[word].get();
        } else if (offset >= PLIC_ENABLE_BASE && offset < PLIC_ENABLE_BASE + PLIC_CONTEXT_COUNT * PLIC_ENABLE_STRIDE) {
            // base + 0x002000: Enable bits for sources 0-31 on context 0
            // base + 0x002004: Enable bits for sources 32-63 on context 0
            // ...
            // base + 0x00207F: Enable bits for sources 992-1023 on context 0
            // base + 0x002080: Enable bits for sources 0-31 on context 1
            // base + 0x002084: Enable bits for sources 32-63 on context 1
            // ...

            final int context = (offset - PLIC_ENABLE_BASE) / PLIC_ENABLE_STRIDE;
            final int contextOffset = offset & (PLIC_ENABLE_STRIDE - 1);
            final int word = contextOffset >>> 2;
            if (word < sourceWords) {
                return enabled[context * sourceWords + word];
            } else {
                LOGGER.debug("invalid enabled read [{}]", Integer.toHexString(offset));
            }

            return 0;
        } else if (offset >= PLIC_CONTEXT_BASE && offset < PLIC_CONTEXT_BASE + PLIC_CONTEXT_COUNT * PLIC_CONTEXT_STRIDE) {
            // base + 0x200000: Priority threshold for context 0
            // base + 0x200004: Claim/complete for context 0
            // base + 0x200008: Reserved
            // ...

            final int context = (offset - PLIC_CONTEXT_BASE) / PLIC_CONTEXT_STRIDE;
            final int contextOffset = offset & (PLIC_CONTEXT_STRIDE - 1);

            if (contextOffset == 0) { // Priority threshold.
                return thresholdByContext[context];
            } else if (contextOffset == 4) { // Claim.
                final int value = claim(context);
                updateInterrupts();
                return value;
            } else {
                LOGGER.debug("invalid context [{}]", context);
            }

            return 0;
        }

        LOGGER.debug("invalid read offset [{}]", Integer.toHexString(offset));
        return 0;
    }

    @Override
    public void store(final int offset, final int value, final int sizeLog2) {
        assert sizeLog2 == 2;

        if (offset >= PLIC_PRIORITY_BASE && offset < PLIC_PRIORITY_BASE + (PLIC_SOURCE_COUNT << 2)) {
            // base + 0x000004: Interrupt source 1 priority
            // base + 0x000008: Interrupt source 2 priority
            // ...
            // base + 0x000FFC: Interrupt source 1023 priority

            final int source = ((offset - PLIC_PRIORITY_BASE) >> 2) + 1; // Plus one because we skip zero.
            priorityBySource[source] = value & PLIC_MAX_PRIORITY;
            updateInterrupts();

            return;
        } else if (offset >= PLIC_PENDING_BASE && offset < PLIC_PENDING_BASE + sourceWords) {
            // base + 0x001000: Interrupt Pending bit 0-31
            // base + 0x00107C: Interrupt Pending bit 992-1023
            // ...

            LOGGER.debug("invalid pending write");
            return;
        } else if (offset >= PLIC_ENABLE_BASE && offset < PLIC_ENABLE_BASE + PLIC_CONTEXT_COUNT * PLIC_ENABLE_STRIDE) {
            // base + 0x002000: Enable bits for sources 0-31 on context 0
            // base + 0x002004: Enable bits for sources 32-63 on context 0
            // ...
            // base + 0x00207F: Enable bits for sources 992-1023 on context 0
            // base + 0x002080: Enable bits for sources 0-31 on context 1
            // base + 0x002084: Enable bits for sources 32-63 on context 1
            // ...

            final int context = (offset - PLIC_ENABLE_BASE) / PLIC_ENABLE_STRIDE;
            final int contextOffset = offset & (PLIC_ENABLE_STRIDE - 1);
            final int word = contextOffset >>> 2;
            if (word < sourceWords) {
                enabled[context * sourceWords + word] = value;
            } else {
                LOGGER.debug("invalid enabled write [{}]", value);
            }

            return;
        } else if (offset >= PLIC_CONTEXT_BASE && offset < PLIC_CONTEXT_BASE + PLIC_CONTEXT_COUNT * PLIC_CONTEXT_STRIDE) {
            // base + 0x200000: Priority threshold for context 0
            // base + 0x200004: Claim/complete for context 0
            // base + 0x200008: Reserved
            // ...

            final int context = (offset - PLIC_CONTEXT_BASE) / PLIC_CONTEXT_STRIDE;
            final int contextOffset = offset & (PLIC_CONTEXT_STRIDE - 1);

            if (contextOffset == 0) { // Priority threshold.
                if (Integer.compareUnsigned(value, PLIC_MAX_PRIORITY) <= 0) {
                    thresholdByContext[context] = value;
                    updateInterrupts();
                } else {
                    LOGGER.debug("invalid threshold write [{}]", value);
                }

                return;
            } else if (contextOffset == 4) { // Complete.
                if (Integer.compareUnsigned(value, PLIC_SOURCE_COUNT) < 0) {
                    setClaimed(value, false);
                    updateInterrupts();
                } else {
                    LOGGER.debug("invalid complete write [{}]", value);
                }

                return;
            } else {
                LOGGER.debug("invalid context [{}]", context);
                return;
            }
        }

        LOGGER.debug("invalid write offset [{}]", Integer.toHexString(offset));
    }

    @Override
    public void raiseInterrupts(int mask) {
        for (int i = 0; mask != 0; i++, mask = mask >>> 1) {
            if ((mask & 0b1) != 0) {
                setPending(i, true);
            }
        }
        updateInterrupts();
    }

    @Override
    public void lowerInterrupts(int mask) {
        for (int i = 0; mask != 0; i++, mask = mask >>> 1) {
            if ((mask & 0b1) != 0) {
                setPending(i, false);
            }
        }
        updateInterrupts();
    }

    @Override
    public int getRaisedInterrupts() {
        return pending[0].get();
    }

    @Override
    public Iterable<Interrupt> getInterrupts() {
        return Arrays.asList(interruptByContext);
    }

    private boolean hasPending(final int context) {
        for (int i = 0; i < sourceWords; i++) {
            final int unclaimed = (pending[i].get() & ~claimed[i].get()) & enabled[context * sourceWords + i];
            if (unclaimed == 0) {
                continue;
            }

            for (int j = 0; j < 32; j++) {
                final int source = (i * 32) + j;
                final int priority = priorityBySource[source];
                final boolean enabled = (unclaimed & (1 << j)) != 0;
                if (enabled && priority > thresholdByContext[context]) {
                    return true;
                }
            }
        }

        return false;
    }

    private void setPending(final int source, final boolean value) {
        final int word = source >>> 5;
        final int mask = 1 << (source & 31);
        if (value) {
            pending[word].updateAndGet(operand -> operand |= mask);
        } else {
            pending[word].updateAndGet(operand -> operand &= ~mask);
        }
    }

    private int claim(final int context) {
        int maxSource = 0;
        int maxPriority = thresholdByContext[context];

        for (int i = 0; i < sourceWords; i++) {
            final int unclaimed = (pending[i].get() & ~claimed[i].get()) & enabled[context * sourceWords + i];
            if (unclaimed == 0) {
                continue;
            }

            for (int j = 0; j < 32; j++) {
                final int source = (i * 32) + j;
                final int priority = priorityBySource[source];
                final boolean enabled = (unclaimed & (1 << j)) != 0;
                if (enabled && priority > maxPriority) {
                    maxSource = source;
                    maxPriority = priority;
                }
            }
        }

        if (maxSource > 0) {
            setPending(maxSource, false);
            setClaimed(maxSource, true);
        }

        return maxSource;
    }

    private void setClaimed(final int source, final boolean value) {
        final int word = source >>> 5;
        final int mask = 1 << (source & 31);
        if (value) {
            claimed[word].updateAndGet(operand -> operand |= mask);
        } else {
            claimed[word].updateAndGet(operand -> operand &= ~mask);
        }
    }

    private void updateInterrupts() {
        for (int context = 0; context < PLIC_CONTEXT_COUNT; context++) {
            if (hasPending(context)) {
                interruptByContext[context].raiseInterrupt();
            } else {
                interruptByContext[context].lowerInterrupt();
            }
        }
    }
}
