package li.cil.oc2.api.bus.device.vm;

public final class VMDeviceLoadResult {
    public static VMDeviceLoadResult success() {
        return new VMDeviceLoadResult(true);
    }

    public static VMDeviceLoadResult fail() {
        return new VMDeviceLoadResult(false);
    }

    private final boolean wasSuccessful;

    private VMDeviceLoadResult(final boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }
}
