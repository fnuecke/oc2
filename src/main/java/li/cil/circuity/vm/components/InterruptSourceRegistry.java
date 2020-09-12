package li.cil.circuity.vm.components;

public final class InterruptSourceRegistry {
    public int registerInterrupt() {
        return -1;
    }

    public boolean registerInterrupt(final int id) {
        return false;
    }

    public void releaseInterrupt(final int id) {
    }
}
