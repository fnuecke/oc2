/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class VMRunnerTests {
    private AbstractArchitecture architecture;

    @BeforeEach
    public void setupEach() {
        architecture = new R5Architecture(new AbstractArchitecture.Config(
            unused -> DeviceLocation.UNSPECIFIED, () -> {
        }, () -> 0L));
    }

    @Test
    public void runContainsUncheckedException() {
        final VMRunner runner = new ThrowingVMRunner(architecture);
        architecture.setRunning(true);

        assertDoesNotThrow(runner::run);

        assertNotNull(runner.getRuntimeError());
        assertFalse(architecture.isRunning());
    }

    @Test
    public void joinDoesNotRethrowOntoCaller() {
        final VMRunner runner = new ThrowingVMRunner(architecture);
        architecture.setRunning(true);

        runner.tick();

        assertDoesNotThrow(runner::join);

        assertNotNull(runner.getRuntimeError());
        assertFalse(architecture.isRunning());
    }

    // --------------------------------------------------------------------- //

    private static final class ThrowingVMRunner extends VMRunner {
        public ThrowingVMRunner(final AbstractArchitecture architecture) {
            super(architecture);
        }

        @Override
        protected void handleBeforeRun() {
            throw new IllegalStateException("device exploded");
        }
    }
}
