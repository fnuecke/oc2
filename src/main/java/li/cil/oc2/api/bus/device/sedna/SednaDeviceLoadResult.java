package li.cil.oc2.api.bus.device.sedna;

public final class SednaDeviceLoadResult {
    public static SednaDeviceLoadResult success() {
        return new SednaDeviceLoadResult(true);
    }

    public static SednaDeviceLoadResult fail() {
        return new SednaDeviceLoadResult(false);
    }

    private final boolean wasSuccessful;

    private SednaDeviceLoadResult(final boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }
}
