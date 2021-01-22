package li.cil.oc2.api.bus.device.vm.event;

import net.minecraft.util.text.ITextComponent;

import java.util.Optional;

public final class VMInitializationException extends Exception {
    private final ITextComponent message;

    public VMInitializationException(final ITextComponent message) {
        this.message = message;
    }

    public VMInitializationException() {
        this.message = null;
    }

    public Optional<ITextComponent> getErrorMessage() {
        return Optional.ofNullable(message);
    }
}
