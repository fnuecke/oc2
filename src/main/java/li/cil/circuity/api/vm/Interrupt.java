package li.cil.circuity.api.vm;

import li.cil.circuity.api.vm.device.InterruptController;

public final class Interrupt {
    public int id;
    public InterruptController controller;

    public Interrupt() {
    }

    public Interrupt(final int id) {
        this.id = id;
    }

    public void raiseInterrupt() {
        if (controller != null) {
            controller.raiseInterrupts(1 << id);
        }
    }

    public void lowerInterrupt() {
        if (controller != null) {
            controller.lowerInterrupts(1 << id);
        }
    }

    public boolean isRaised() {
        return controller != null && ((controller.getRaisedInterrupts() & (1 << id)) != 0);
    }
}
