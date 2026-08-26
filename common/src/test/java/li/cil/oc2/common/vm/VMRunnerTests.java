/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class VMRunnerTests {
    private TestVirtualMachine virtualMachine;

    @BeforeEach
    public void setupEach() {
        final DeviceBusElement busElement = mock(DeviceBusElement.class);
        when(busElement.getLocalDevices()).thenReturn(emptyList());
        when(busElement.getNeighbors()).thenReturn(Optional.empty());

        virtualMachine = new TestVirtualMachine(new CommonDeviceBusController(busElement, 0));
    }

    @Test
    public void runContainsUncheckedException() {
        final VMRunner runner = new ThrowingVMRunner(virtualMachine);
        virtualMachine.state.board.setRunning(true);

        assertDoesNotThrow(runner::run);

        assertNotNull(runner.getRuntimeError());
        assertFalse(virtualMachine.state.board.isRunning());
    }

    @Test
    public void joinDoesNotRethrowOntoCaller() {
        final VMRunner runner = new ThrowingVMRunner(virtualMachine);
        virtualMachine.state.board.setRunning(true);

        runner.tick();

        assertDoesNotThrow(runner::join);

        assertNotNull(runner.getRuntimeError());
        assertFalse(virtualMachine.state.board.isRunning());
    }

    // --------------------------------------------------------------------- //

    private static final class TestVirtualMachine extends AbstractVirtualMachine {
        public TestVirtualMachine(final CommonDeviceBusController busController) {
            super(busController);
        }

        @Override
        protected AbstractTerminalVMRunner createRunner() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean consumeEnergy(final int amount, final boolean simulate) {
            return true;
        }
    }

    private static final class ThrowingVMRunner extends VMRunner {
        public ThrowingVMRunner(final AbstractVirtualMachine virtualMachine) {
            super(virtualMachine);
        }

        @Override
        protected void handleBeforeRun() {
            throw new IllegalStateException("device exploded");
        }
    }
}
