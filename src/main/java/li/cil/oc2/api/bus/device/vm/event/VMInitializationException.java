package li.cil.oc2.api.bus.device.vm.event;

import net.minecraft.util.text.ITextComponent;

import java.util.Optional;

/**
 * May be fired by devices while handling {@link VMInitializingEvent} to indicate that initialization failed.
 */
public final class VMInitializationException extends RuntimeException {
    private final ITextComponent message;

    ///////////////////////////////////////////////////////////////

    public VMInitializationException(final ITextComponent message) {
        this.message = message;
    }

    public VMInitializationException() {
        this.message = null;
    }

    ///////////////////////////////////////////////////////////////

    /**
     * The error message indicating why initialization failed.
     * <p>
     * This should be a human readable message, as it may be displayed to the user.
     *
     * @return the error message.
     */
    public Optional<ITextComponent> getErrorMessage() {
        return Optional.ofNullable(message);
    }
}
