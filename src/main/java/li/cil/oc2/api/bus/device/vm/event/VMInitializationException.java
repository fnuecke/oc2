package li.cil.oc2.api.bus.device.vm.event;

import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class VMInitializationException extends RuntimeException {
    private final Component message;

    public VMInitializationException(final Component message) {
        this.message = message;
    }

    public VMInitializationException() {
        this.message = null;
    }

    public Optional<Component> getErrorMessage() {
        return Optional.ofNullable(message);
    }
}
