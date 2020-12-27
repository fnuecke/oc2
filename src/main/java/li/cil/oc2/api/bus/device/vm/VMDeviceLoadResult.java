package li.cil.oc2.api.bus.device.vm;

import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public final class VMDeviceLoadResult {
    public static VMDeviceLoadResult success() {
        return new VMDeviceLoadResult(true);
    }

    public static VMDeviceLoadResult fail() {
        return new VMDeviceLoadResult(false);
    }

    private final boolean wasSuccessful;
    @Nullable private ITextComponent message;

    private VMDeviceLoadResult(final boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }

    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public VMDeviceLoadResult withErrorMessage(final ITextComponent value) {
        message = value;
        return this;
    }

    @Nullable
    public ITextComponent getErrorMessage() {
        return message;
    }
}
